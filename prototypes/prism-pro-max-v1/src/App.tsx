import { useEffect, useMemo, useRef, useState } from 'react'
import './App.css'
import {
  ArrowUpRight,
  AudioLines,
  CalendarDays,
  ChevronDown,
  ChevronRight,
  Clock3,
  FileStack,
  Menu,
  Mic,
  MoveRight,
  Plus,
  Sparkles,
  Wifi,
} from 'lucide-react'

type AgendaItem = {
  id: string
  title: string
  time: string
  duration: string
  owner: string
  state: 'focus' | 'normal' | 'conflict' | 'done'
  note: string
}

type DayItem = {
  label: string
  date: string
  state: 'active' | 'busy' | 'normal'
}

const FRAME_HEIGHT = 812
const DRAWER_MIN = 18
const DRAWER_MAX = 84
const DRAWER_PEEK = 24
const DRAWER_EXPANDED = 78

const dayItems: DayItem[] = [
  { label: 'Mon', date: '18', state: 'normal' },
  { label: 'Tue', date: '19', state: 'busy' },
  { label: 'Wed', date: '20', state: 'active' },
  { label: 'Thu', date: '21', state: 'busy' },
  { label: 'Fri', date: '22', state: 'normal' },
  { label: 'Sat', date: '23', state: 'normal' },
]

const agendaItems: AgendaItem[] = [
  {
    id: '1',
    title: 'A3 procurement alignment',
    time: '09:30',
    duration: '45 min',
    owner: 'Frank · Phone call',
    state: 'focus',
    note: 'Prepare pricing guardrails and competitor objections before call.',
  },
  {
    id: '2',
    title: 'Badge transcript review',
    time: '11:00',
    duration: '25 min',
    owner: 'AI summary lane',
    state: 'normal',
    note: 'Convert overnight recording into a customer-ready follow-up summary.',
  },
  {
    id: '3',
    title: 'Factory visit scheduling conflict',
    time: '14:30',
    duration: '60 min',
    owner: 'Operations + Sales',
    state: 'conflict',
    note: 'Conflicts with regional review. Recommend push to 16:00 or split attendees.',
  },
  {
    id: '4',
    title: 'Key account recap sent',
    time: '18:10',
    duration: 'Done',
    owner: 'Sent from Prism',
    state: 'done',
    note: 'Client received recap and next-step document bundle.',
  },
]

const visualRules = [
  {
    title: 'Aurora night glass',
    body: 'Deep navy base, soft cyan and violet light fields, 22–30px backdrop blur, and 1px high-contrast borders.',
  },
  {
    title: 'Executive density',
    body: 'Every card carries one primary decision: schedule, brief, transcript, or next action. No decorative chrome.',
  },
  {
    title: 'Action hierarchy',
    body: 'Blue = primary action, violet = AI context, amber = caution, green = completed state. Never mix semantic channels.',
  },
  {
    title: 'Motion discipline',
    body: '240ms ease-out by default, drag only for the scheduler drawer, and reduced-motion friendly ambient lighting.',
  },
]

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function App() {
  const [drawerHeight, setDrawerHeight] = useState(DRAWER_PEEK)
  const [isDragging, setIsDragging] = useState(false)
  const startRef = useRef({ y: 0, height: DRAWER_PEEK })

  const drawerProgress = (drawerHeight - DRAWER_MIN) / (DRAWER_MAX - DRAWER_MIN)
  const contentProgress = clamp((drawerProgress - 0.08) / 0.92, 0, 1)
  const scrimOpacity = clamp(drawerProgress * 0.24, 0, 0.24)
  const schedulerOpen = drawerHeight > DRAWER_PEEK + 8

  useEffect(() => {
    if (!isDragging) return

    const handleMove = (event: PointerEvent) => {
      const delta = event.clientY - startRef.current.y
      const nextHeight = startRef.current.height + (delta / FRAME_HEIGHT) * 100
      setDrawerHeight(clamp(nextHeight, DRAWER_MIN, DRAWER_MAX))
    }

    const handleUp = () => {
      setIsDragging(false)
      const snapTargets = [DRAWER_MIN, DRAWER_PEEK, 42, DRAWER_EXPANDED]
      const nearest = snapTargets.reduce((prev, current) =>
        Math.abs(current - drawerHeight) < Math.abs(prev - drawerHeight) ? current : prev
      )
      setDrawerHeight(nearest)
    }

    window.addEventListener('pointermove', handleMove)
    window.addEventListener('pointerup', handleUp)

    return () => {
      window.removeEventListener('pointermove', handleMove)
      window.removeEventListener('pointerup', handleUp)
    }
  }, [drawerHeight, isDragging])

  const nextMeeting = useMemo(
    () => agendaItems.find((item) => item.state === 'focus') ?? agendaItems[0],
    []
  )

  const completedCount = agendaItems.filter((item) => item.state === 'done').length
  const openCount = agendaItems.filter((item) => item.state !== 'done').length

  return (
    <main className="prototype-shell">
      <section className="prototype-brief glass-panel">
        <div className="brief-kicker">Prism Glass Prototype</div>
        <h1>Modern, sleek, glassmorphism homepage plus scheduler drawer.</h1>
        <p>
          I would make Prism feel like an executive ambient workspace: a calm night aurora,
          frosted operating surfaces, dense but readable cards, and a top scheduler drawer that
          feels native on both touch and mouse.
        </p>

        <div className="brief-actions">
          <button className="primary-button" onClick={() => setDrawerHeight(DRAWER_EXPANDED)}>
            Open scheduler
          </button>
          <button className="secondary-button" onClick={() => setDrawerHeight(DRAWER_MIN)}>
            Show homepage
          </button>
        </div>

        <div className="rule-grid">
          {visualRules.map((rule) => (
            <article key={rule.title} className="rule-card glass-panel">
              <div className="rule-dot" />
              <h2>{rule.title}</h2>
              <p>{rule.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="phone-frame">
        <div className="phone-screen">
          <div className="aurora aurora-a" />
          <div className="aurora aurora-b" />
          <div className="aurora aurora-c" />

          <div className="status-bar">
            <span>9:41</span>
            <span>Prism Executive Desk</span>
            <span>5G 92%</span>
          </div>

          <div className="home-layer">
            <header className="home-header glass-panel">
              <div className="header-left">
                <button className="icon-button" aria-label="Menu">
                  <Menu size={18} />
                </button>
                <button className="icon-button" aria-label="Connectivity">
                  <Wifi size={18} />
                </button>
              </div>

              <div className="header-title">
                <span className="eyebrow">Current session</span>
                <strong>CEO Wang · Procurement rhythm</strong>
              </div>

              <div className="header-right">
                <button className="pill-button subtle">Debug</button>
                <button className="icon-button accent" aria-label="New session">
                  <Plus size={18} />
                </button>
              </div>
            </header>

            <aside className="side-rail">
              <button className="rail-chip glass-panel">
                <Sparkles size={16} />
                <span>Insights</span>
              </button>
              <button className="rail-chip glass-panel">
                <FileStack size={16} />
                <span>Files</span>
              </button>
            </aside>

            <section className="hero-card glass-panel">
              <div className="eyebrow accent-text">Today overview</div>
              <h2>Good afternoon, Frank.</h2>
              <p>
                You have {openCount} active lanes. The most time-sensitive meeting is already
                grounded with AI notes and a conflict recommendation.
              </p>

              <div className="hero-stats">
                <div className="metric-chip glass-panel">
                  <span className="metric-label">Open</span>
                  <strong>{openCount}</strong>
                </div>
                <div className="metric-chip glass-panel">
                  <span className="metric-label">Completed</span>
                  <strong>{completedCount}</strong>
                </div>
                <div className="metric-chip glass-panel">
                  <span className="metric-label">Focus</span>
                  <strong>1</strong>
                </div>
              </div>
            </section>

            <section className="brief-strip glass-panel">
              <div className="strip-icon">
                <CalendarDays size={16} />
              </div>
              <div className="strip-copy">
                <span className="eyebrow">Next meeting</span>
                <strong>{nextMeeting.title}</strong>
                <p>{nextMeeting.note}</p>
              </div>
              <button className="ghost-link">
                View brief
                <ArrowUpRight size={14} />
              </button>
            </section>

            <section className="insight-stack">
              <article className="info-card glass-panel emphasis">
                <div className="card-row">
                  <div>
                    <span className="eyebrow accent-text">AI brief</span>
                    <h3>Account pulse is ready before the 09:30 call</h3>
                  </div>
                  <span className="state-pill violet">Analyst</span>
                </div>
                <p>
                  Generated from badge transcript, session context, and last quarter pricing
                  friction notes.
                </p>
                <div className="card-actions">
                  <button className="primary-button small">Open summary</button>
                  <button className="secondary-button small">Adjust focus</button>
                </div>
              </article>

              <article className="info-card glass-panel">
                <div className="card-row">
                  <div>
                    <span className="eyebrow">Smart audio</span>
                    <h3>Overnight recording is ready for structured follow-up</h3>
                  </div>
                  <span className="state-pill blue">Audio</span>
                </div>
                <p>
                  Transcript, summary, chaptering, and sales-action extraction are staged in one
                  surface.
                </p>
              </article>
            </section>

            <div className="bottom-dock glass-panel">
              <button className="attach-button" aria-label="Attach">
                <Plus size={18} />
              </button>

              <div className="dock-input">
                <span className="eyebrow">Assistant input</span>
                <strong>Type a task, ask for analysis, or hold the badge key to speak</strong>
              </div>

              <div className="dock-mode">Coach</div>

              <button className="send-button" aria-label="Voice input">
                <Mic size={18} />
              </button>
            </div>
          </div>

          <div className="home-scrim" style={{ opacity: scrimOpacity }} />

          <section
            className={`scheduler-drawer ${schedulerOpen ? 'is-open' : ''}`}
            style={{ height: `${drawerHeight}%` }}
          >
            <div
              className="drawer-handle-zone"
              onPointerDown={(event) => {
                setIsDragging(true)
                startRef.current = { y: event.clientY, height: drawerHeight }
              }}
            >
              <div className="drawer-handle" />
              <div className="drawer-peek">
                <span className="eyebrow">Scheduler drawer</span>
                <button
                  className="mini-toggle"
                  onClick={() =>
                    setDrawerHeight((current) =>
                      current > DRAWER_PEEK + 8 ? DRAWER_MIN : DRAWER_EXPANDED
                    )
                  }
                >
                  {schedulerOpen ? 'Collapse' : 'Expand'}
                  <ChevronDown size={14} className={schedulerOpen ? 'rotated' : ''} />
                </button>
              </div>
            </div>

            <div className="drawer-content" style={{ opacity: contentProgress }}>
              <div className="drawer-topline">
                <div>
                  <span className="eyebrow accent-text">Wednesday focus</span>
                  <h2>March 20 · Executive schedule</h2>
                </div>
                <button className="state-pill blue">
                  AI planning
                  <MoveRight size={14} />
                </button>
              </div>

              <div className="month-strip">
                {['Feb', 'Mar', 'Apr', 'May'].map((month) => (
                  <button
                    key={month}
                    className={`month-pill ${month === 'Mar' ? 'active' : ''}`}
                  >
                    {month}
                  </button>
                ))}
              </div>

              <div className="day-strip">
                {dayItems.map((day) => (
                  <button
                    key={`${day.label}-${day.date}`}
                    className={`day-pill day-${day.state}`}
                  >
                    <span>{day.label}</span>
                    <strong>{day.date}</strong>
                  </button>
                ))}
              </div>

              <div className="drawer-banner glass-panel">
                <div className="banner-copy">
                  <span className="eyebrow">Fast-track lane</span>
                  <strong>One exact reminder is ready to post after approval.</strong>
                </div>
                <button className="ghost-link">
                  Review
                  <ChevronRight size={14} />
                </button>
              </div>

              <div className="agenda-list">
                {agendaItems.map((item) => (
                  <article key={item.id} className={`agenda-card agenda-${item.state}`}>
                    <div className="agenda-time">
                      <span>{item.time}</span>
                      <small>{item.duration}</small>
                    </div>
                    <div className="agenda-body">
                      <div className="card-row">
                        <div>
                          <h3>{item.title}</h3>
                          <span>{item.owner}</span>
                        </div>
                        <div className={`state-pill ${item.state}`}>
                          {item.state === 'focus' && 'Priority'}
                          {item.state === 'normal' && 'Planned'}
                          {item.state === 'conflict' && 'Conflict'}
                          {item.state === 'done' && 'Done'}
                        </div>
                      </div>
                      <p>{item.note}</p>

                      {item.state === 'conflict' && (
                        <div className="conflict-actions">
                          <button className="primary-button small">Move to 16:00</button>
                          <button className="secondary-button small">Split attendees</button>
                        </div>
                      )}
                    </div>
                  </article>
                ))}
              </div>

              <div className="drawer-composer glass-panel">
                <div className="composer-icon">
                  <AudioLines size={16} />
                </div>
                <div className="composer-copy">
                  <span className="eyebrow">Scheduler input</span>
                  <strong>Speak or type to create an exact reminder or save an inspiration.</strong>
                </div>
                <button className="send-button compact" aria-label="Start audio">
                  <Clock3 size={16} />
                </button>
              </div>
            </div>
          </section>
        </div>
      </section>
    </main>
  )
}

export default App
