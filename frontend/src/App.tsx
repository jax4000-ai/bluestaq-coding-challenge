import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import Dashboard from './Dashboard'
import './App.css'

type NoteStatus = 'ACTIVE' | 'ARCHIVED'
type NoteSort = 'UPDATED_DESC' | 'CREATED_DESC' | 'TITLE_ASC'
type DataClassification = 'PUBLIC' | 'INTERNAL' | 'CUI'

type Note = {
  id: number
  teamId: string
  authorId: string
  title: string
  content: string
  status: NoteStatus
  classification: DataClassification
  createdAt: string
  updatedAt: string
  version: number
}

type Problem = {
  title?: string
  detail?: string
}

const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

const CLEARANCE_RANK: Record<DataClassification, number> = { PUBLIC: 0, INTERNAL: 1, CUI: 2 }

function isClassificationAllowed(clearance: DataClassification, classification: DataClassification) {
  return CLEARANCE_RANK[clearance] >= CLEARANCE_RANK[classification]
}

/**
 * DEMO-ONLY passphrases that gate the clearance selector in the browser. This is not a real
 * security boundary: the passphrases are intentionally visible in this file and in the UI, and
 * anyone calling the API directly can still set any `X-User-Clearance` header (see the README's
 * "Demo identity boundary" section). Its only purpose is to stop a casual click from instantly
 * revealing higher-classification notes.
 */
const CLEARANCE_PASSPHRASES: Partial<Record<DataClassification, string>> = {
  INTERNAL: 'internal-demo',
  CUI: 'cui-demo',
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Problem
    throw new Error(problem.detail ?? problem.title ?? `Request failed (${response.status})`)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

function App() {
  const [view, setView] = useState<'notes' | 'dashboard'>('notes')
  const [teamId, setTeamId] = useState('orbital-ops')
  const [authorId, setAuthorId] = useState('operator.ada')
  const [clearance, setClearance] = useState<DataClassification>('PUBLIC')
  const [classification, setClassification] = useState<DataClassification>('PUBLIC')
  const [clearanceNotice, setClearanceNotice] = useState('')
  const [pendingClearance, setPendingClearance] = useState<DataClassification | null>(null)
  const [passphraseInput, setPassphraseInput] = useState('')
  const [notes, setNotes] = useState<Note[]>([])
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<NoteStatus>('ACTIVE')
  const [sort, setSort] = useState<NoteSort>('UPDATED_DESC')
  const [selected, setSelected] = useState<Note | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const notesUrl = `${API_BASE}/api/teams/${encodeURIComponent(teamId)}/notes`
  const identityHeaders = useMemo(
    () => ({
      'X-User-Id': authorId,
      'X-User-Clearance': clearance,
    }),
    [authorId, clearance],
  )

  const loadNotes = useCallback(async () => {
    setLoading(true)
    const params = new URLSearchParams({ query, status, sort })
    try {
      const loaded = await request<Note[]>(`${notesUrl}?${params}`, {
        headers: identityHeaders,
      })
      setNotes(loaded)
      setError('')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not load notes')
    } finally {
      setLoading(false)
    }
  }, [identityHeaders, notesUrl, query, sort, status])

  useEffect(() => {
    const timer = window.setTimeout(loadNotes, 200)
    return () => window.clearTimeout(timer)
  }, [loadNotes])

  useEffect(() => {
    const eventParams = new URLSearchParams({ userId: authorId, clearance })
    const events = new EventSource(`${notesUrl}/events?${eventParams}`)
    events.onmessage = () => loadNotes()
    events.addEventListener('created', () => loadNotes())
    events.addEventListener('updated', () => loadNotes())
    events.addEventListener('archived', () => loadNotes())
    events.addEventListener('restored', () => loadNotes())
    events.addEventListener('deleted', () => loadNotes())
    return () => events.close()
  }, [authorId, clearance, loadNotes, notesUrl])

  function startNew() {
    setSelected(null)
    setTitle('')
    setContent('')
    setError('')
  }

  function applyClearance(nextClearance: DataClassification) {
    setClearance(nextClearance)
    if (!isClassificationAllowed(nextClearance, classification)) {
      setClassification(nextClearance)
    }
    startNew()
  }

  function requestClearanceChange(nextClearance: DataClassification) {
    const requiredPassphrase = CLEARANCE_PASSPHRASES[nextClearance]

    if (!requiredPassphrase) {
      setPendingClearance(null)
      setClearanceNotice('')
      applyClearance(nextClearance)
      return
    }

    setPendingClearance(nextClearance)
    setPassphraseInput('')
    setClearanceNotice('')
  }

  function confirmPassphrase(event: FormEvent) {
    event.preventDefault()
    if (!pendingClearance) return

    if (passphraseInput === CLEARANCE_PASSPHRASES[pendingClearance]) {
      applyClearance(pendingClearance)
      setPendingClearance(null)
      setPassphraseInput('')
      setClearanceNotice('')
    } else {
      setClearanceNotice(`Incorrect demo passphrase for ${pendingClearance}. Try again or cancel.`)
    }
  }

  function cancelPassphrase() {
    setPendingClearance(null)
    setPassphraseInput('')
    setClearanceNotice('')
  }

  function startEdit(note: Note) {
    setSelected(note)
    setTitle(note.title)
    setContent(note.content)
    setError('')
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      if (selected) {
        await request<Note>(`${notesUrl}/${selected.id}`, {
          method: 'PUT',
          headers: identityHeaders,
          body: JSON.stringify({ title, content, version: selected.version }),
        })
      } else {
        await request<Note>(notesUrl, {
          method: 'POST',
          headers: identityHeaders,
          body: JSON.stringify({ title, content, classification }),
        })
      }
      startNew()
      await loadNotes()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not save note')
    } finally {
      setSaving(false)
    }
  }

  async function toggleArchive(note: Note) {
    setError('')
    try {
      await request<Note>(
        `${notesUrl}/${note.id}/archive?archived=${note.status === 'ACTIVE'}`,
        { method: 'PATCH', headers: identityHeaders },
      )
      if (selected?.id === note.id) startNew()
      await loadNotes()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not update note')
    }
  }

  async function deleteNote(note: Note) {
    if (!window.confirm(`Delete "${note.title}" permanently?`)) return
    setError('')
    try {
      await request<void>(`${notesUrl}/${note.id}`, {
        method: 'DELETE',
        headers: identityHeaders,
      })
      if (selected?.id === note.id) startNew()
      await loadNotes()
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Could not delete note')
    }
  }

  return (
    <main className="app-shell">
      <div className="demo-notice" role="note">
        <strong>INTERVIEW DEMO</strong>
        Synthetic data only. Do not enter real government, CUI, customer, or personal information.
      </div>
      <div className="demo-notice demo-notice-security" role="note">
        <strong>SIMULATED ACCESS CONTROL</strong>
        Clearance below is a self-selected demo control gated by a visible passphrase, not a real
        login. Anyone calling the API directly can still set any clearance header. See the
        README&apos;s &quot;Demo identity boundary&quot; section for the production security model.
      </div>
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">N</span>
          <div>
            <strong>Mission Notes</strong>
            <span>Controlled collaboration for mission teams.</span>
          </div>
        </div>
        <div className="access-controls">
          <div className="view-tabs" role="tablist" aria-label="View">
            <button
              type="button"
              role="tab"
              aria-selected={view === 'notes'}
              className={view === 'notes' ? 'active' : ''}
              onClick={() => setView('notes')}
            >
              Notes
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={view === 'dashboard'}
              className={view === 'dashboard' ? 'active' : ''}
              onClick={() => setView('dashboard')}
            >
              Dashboard
            </button>
          </div>
          <label className="team-switcher">
            Mission team
            <input
              value={teamId}
              onChange={(event) => {
                setTeamId(event.target.value || 'orbital-ops')
                startNew()
              }}
              maxLength={64}
            />
          </label>
          <label className="team-switcher">
            Clearance
            <select value={clearance} onChange={(event) => requestClearanceChange(event.target.value as DataClassification)}>
              <option value="PUBLIC">Public</option>
              <option value="INTERNAL">Internal (passphrase)</option>
              <option value="CUI">CUI (passphrase)</option>
            </select>
          </label>
        </div>
      </header>

      {pendingClearance && (
        <form
          className="clearance-gate"
          onSubmit={confirmPassphrase}
          role="alertdialog"
          aria-label={`Unlock ${pendingClearance} clearance`}
        >
          <div className="clearance-gate-copy">
            <strong>Demo passphrase required for {pendingClearance}</strong>
            <span>
              UI convenience only, not real security &mdash; the passphrase is intentionally
              visible below and in the README.
            </span>
          </div>
          <input
            type="password"
            autoFocus
            value={passphraseInput}
            onChange={(event) => setPassphraseInput(event.target.value)}
            placeholder="Demo passphrase"
            aria-label="Demo passphrase"
          />
          <div className="clearance-gate-actions">
            <button type="submit">Unlock</button>
            <button type="button" className="text-button" onClick={cancelPassphrase}>
              Cancel
            </button>
          </div>
          <span className="clearance-gate-hint">
            Hint: <code>{CLEARANCE_PASSPHRASES[pendingClearance]}</code>
          </span>
        </form>
      )}

      {clearanceNotice && (
        <div className="clearance-notice" role="alert">
          {clearanceNotice}
        </div>
      )}

      {view === 'dashboard' ? (
        <Dashboard apiBase={API_BASE} />
      ) : (
        <>
          <section className="hero">
            <div>
              <p className="eyebrow">Authorized use only · {clearance} workspace</p>
              <h1>Mission context.<br />Need-to-know access.</h1>
              <p className="hero-copy">
                Classification-aware collaboration with auditable changes and live team updates.
              </p>
            </div>
            <div className="hero-stat">
              <strong>{notes.length}</strong>
              <span>{status.toLowerCase()} notes</span>
            </div>
          </section>

          <section className="workspace">
        <aside className="note-list-panel">
          <div className="toolbar">
            <input
              className="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search notes..."
              aria-label="Search notes"
            />
            <button className="new-button" type="button" onClick={startNew}>
              + New note
            </button>
          </div>

          <div className="filters">
            <div className="tabs" aria-label="Note status">
              {(['ACTIVE', 'ARCHIVED'] as NoteStatus[]).map((option) => (
                <button
                  key={option}
                  className={status === option ? 'active' : ''}
                  type="button"
                  onClick={() => setStatus(option)}
                >
                  {option === 'ACTIVE' ? 'Active' : 'Archive'}
                </button>
              ))}
            </div>
            <select
              value={sort}
              onChange={(event) => setSort(event.target.value as NoteSort)}
              aria-label="Sort notes"
            >
              <option value="UPDATED_DESC">Recently updated</option>
              <option value="CREATED_DESC">Recently created</option>
              <option value="TITLE_ASC">Title A–Z</option>
            </select>
          </div>

          <div className="notes">
            {loading ? (
              <p className="empty">Loading notes...</p>
            ) : notes.length === 0 ? (
              <div className="empty">
                <span>✦</span>
                <strong>No notes here yet</strong>
                <p>Create one or change your search.</p>
              </div>
            ) : (
              notes.map((note) => (
                <article
                  key={note.id}
                  className={`note-card ${selected?.id === note.id ? 'selected' : ''}`}
                  onClick={() => startEdit(note)}
                >
                  <div>
                    <span className={`status-dot ${note.status.toLowerCase()}`} />
                    <time>{new Date(note.updatedAt).toLocaleDateString()}</time>
                  </div>
                  <h2>{note.title}</h2>
                  <span className={`classification ${note.classification.toLowerCase()}`}>
                    {note.classification}
                  </span>
                  <p>{note.content}</p>
                  <footer>
                    <span>by {note.authorId}</span>
                    <div>
                      <button
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation()
                          toggleArchive(note)
                        }}
                      >
                        {note.status === 'ACTIVE' ? 'Archive' : 'Restore'}
                      </button>
                      <button
                        className="danger"
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation()
                          deleteNote(note)
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </footer>
                </article>
              ))
            )}
          </div>
        </aside>

        <section className="editor-panel">
          <div className="editor-heading">
            <div>
              <p className="eyebrow">{selected ? 'Editing note' : 'New note'}</p>
              <h2>{selected ? selected.title : 'Capture mission context'}</h2>
            </div>
            {selected && (
              <button className="text-button" type="button" onClick={startNew}>
                Close
              </button>
            )}
          </div>

          <form onSubmit={save}>
            {!selected && (
              <label>
                Operator identity
                <input
                  value={authorId}
                  onChange={(event) => setAuthorId(event.target.value)}
                  required
                  maxLength={64}
                />
              </label>
            )}
            {!selected && (
              <label>
                Data marking
                <select
                  value={classification}
                  onChange={(event) =>
                    setClassification(event.target.value as DataClassification)
                  }
                >
                  <option value="PUBLIC">PUBLIC</option>
                  <option value="INTERNAL" disabled={!isClassificationAllowed(clearance, 'INTERNAL')}>INTERNAL</option>
                  <option value="CUI" disabled={!isClassificationAllowed(clearance, 'CUI')}>CUI</option>
                </select>
              </label>
            )}
            <label>
              Title
              <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="e.g. Sprint retrospective"
                required
                maxLength={120}
              />
            </label>
            <label>
              Note
              <textarea
                value={content}
                onChange={(event) => setContent(event.target.value)}
                placeholder="Capture the context, decision, and next step..."
                required
                maxLength={10000}
              />
            </label>
            {error && <div className="error" role="alert">{error}</div>}
            <div className="form-footer">
              <span>{content.length.toLocaleString()} / 10,000</span>
              <button className="save-button" type="submit" disabled={saving}>
                {saving ? 'Saving...' : selected ? 'Save changes' : 'Create note'}
              </button>
            </div>
          </form>

          <div className="engineering-note">
            <span>CONTROLLED</span>
            Access is filtered server-side by team and clearance. Mutations are audited; stale edits are rejected.
          </div>
        </section>
          </section>
        </>
      )}
    </main>
  )
}

export default App
