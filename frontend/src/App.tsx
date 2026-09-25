import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
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
  const [teamId, setTeamId] = useState('orbital-ops')
  const [authorId, setAuthorId] = useState('operator.ada')
  const [clearance, setClearance] = useState<DataClassification>('CUI')
  const [classification, setClassification] = useState<DataClassification>('INTERNAL')
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
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">N</span>
          <div>
            <strong>Mission Notes</strong>
            <span>Controlled collaboration for mission teams.</span>
          </div>
        </div>
        <div className="access-controls">
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
            <select
              value={clearance}
              onChange={(event) => {
                const nextClearance = event.target.value as DataClassification
                setClearance(nextClearance)
                if (classification === 'CUI' && nextClearance !== 'CUI') {
                  setClassification('INTERNAL')
                }
                startNew()
              }}
            >
              <option value="PUBLIC">Public</option>
              <option value="INTERNAL">Internal</option>
              <option value="CUI">CUI</option>
            </select>
          </label>
        </div>
      </header>

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
                  <option value="INTERNAL">INTERNAL</option>
                  <option value="CUI" disabled={clearance !== 'CUI'}>CUI</option>
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
    </main>
  )
}

export default App
