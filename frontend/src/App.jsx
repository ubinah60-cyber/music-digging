import React, { useRef, useState } from 'react'

function Icon({ name, size = 28 }) {
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 32 32',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.7,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
  }

  if (name === 'search') {
    return <svg {...common}><circle cx="14" cy="14" r="9.4" /><path d="m21.2 21.2 7 7" /></svg>
  }
  if (name === 'home') {
    return <svg {...common} fill="currentColor" stroke="none"><path d="M3.2 14.3 16 3.7l12.8 10.6v13a1.6 1.6 0 0 1-1.6 1.6h-7.1v-9.4h-8.2v9.4H4.8a1.6 1.6 0 0 1-1.6-1.6z" /></svg>
  }
  if (name === 'dig') {
    return <svg {...common}><path d="M15 2.8c0 8.1-3.5 12.1-11.3 13.2C11.5 17.1 15 21.1 15 29.2c0-8.1 3.5-12.1 11.3-13.2C18.5 14.9 15 10.9 15 2.8Z" /><path d="M25.5 22.1v6.7m-3.3-3.4h6.6" /></svg>
  }
  if (name === 'community') {
    return <svg {...common}><circle cx="11" cy="9.5" r="4.2" /><path d="M2.6 27.5v-3.7a7.2 7.2 0 0 1 7.2-7.2h2.5a7.2 7.2 0 0 1 7.2 7.2v3.7H2.6Z" /><path d="M22.5 8.2a3.6 3.6 0 1 1 0 7.2m.2 2.5a6.1 6.1 0 0 1 6.1 6.1v3.5h-6" /></svg>
  }
  return <svg {...common}><circle cx="16" cy="9.2" r="5.1" /><path d="M5.1 28.2v-2.1a10.9 10.9 0 0 1 21.8 0v2.1" /></svg>
}

const scenes = {
  home: {
    paths: [
      'M330 438 C 470 319, 602 307, 742 330 S 1055 452, 1242 246 S 1423 176, 1465 164',
      'M330 438 C 520 494, 648 300, 742 330 S 930 367, 1242 246',
      'M576 246 C 658 316, 690 315, 742 330 C 878 230, 1073 172, 1242 246 C 1360 289, 1411 350, 1464 406',
      'M742 330 C 838 355, 932 399, 1020 428 C 1190 488, 1367 481, 1464 406',
      'M1242 246 C 1329 185, 1404 174, 1465 164',
    ],
    stars: [[516,326],[661,206],[1030,281],[1046,192],[1336,186],[1492,235],[1580,167],[954,383],[1535,308]],
    nodes: [[334,438,6],[576,246,5],[747,330,18],[1020,428,7],[1242,246,11],[1464,406,6],[1465,164,8]],
  },
  search: {
    paths: [
      'M167 363 C 282 220, 401 195, 473 213 C 568 246, 610 279, 695 254 C 858 177, 1008 177, 1132 241 C 1250 294, 1366 235, 1437 152',
      'M167 363 C 340 383, 530 256, 695 254 S 1010 344, 1132 241 S 1353 229, 1437 152',
      'M473 213 C 587 280, 623 276, 695 254 C 872 274, 1030 297, 1132 241 C 1232 261, 1310 306, 1338 340',
      'M695 254 C 845 347, 1101 388, 1338 340 C 1376 325, 1417 215, 1437 152',
    ],
    stars: [[110,307],[286,230],[429,403],[598,128],[939,266],[1097,113],[1500,66],[1554,218],[1501,355]],
    nodes: [[167,363,6],[473,213,5],[695,254,12],[1132,241,6],[1338,340,5],[1437,152,6]],
  },
}

function Constellation({ variant }) {
  const scene = scenes[variant]
  const glowId = `${variant}-nodeGlow`
  const softId = `${variant}-softGlow`
  const smallId = `${variant}-smallGlow`

  return (
    <svg className={`constellation constellation-${variant}`} viewBox="0 0 1680 590" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs>
        <radialGradient id={glowId}><stop stopColor="#fff" stopOpacity=".48" /><stop offset=".12" stopColor="#efe8ff" stopOpacity=".2" /><stop offset="1" stopColor="#fff" stopOpacity="0" /></radialGradient>
        <filter id={softId} x="-400%" y="-400%" width="900%" height="900%"><feGaussianBlur stdDeviation="9" /></filter>
        <filter id={smallId} x="-200%" y="-200%" width="500%" height="500%"><feGaussianBlur stdDeviation="3" /></filter>
      </defs>
      <g className="space-drift">
        <g className="orbit-lines">
          {scene.paths.map((path) => <path key={path} d={path} />)}
        </g>
        <g className="orbit-trails">
          <path d={scene.paths[0]} pathLength="1000" />
          <path d={scene.paths[1]} pathLength="1000" />
        </g>
        <g className="stars">
          {scene.stars.map(([x, y], index) => <circle key={`${x}-${y}`} cx={x} cy={y} r={index % 3 === 0 ? 1.4 : 1.1} style={{ animationDelay: `${index * -.7}s` }} />)}
        </g>
        {scene.nodes.map(([x,y,r], index) => (
          <g className="node" key={`${x}-${y}`} style={{ animationDelay: `${index * -.65}s` }}>
            <circle cx={x} cy={y} r={r * 9} fill={`url(#${glowId})`} />
            <circle cx={x} cy={y} r={r * 2.8} fill="#fff" opacity=".62" filter={`url(#${softId})`} />
            <circle cx={x} cy={y} r={r * 1.35} fill="#fff" opacity=".75" filter={`url(#${smallId})`} />
            <circle cx={x} cy={y} r={r} fill="#fff" />
          </g>
        ))}
      </g>
    </svg>
  )
}

const navItems = [
  { name: '메인', icon: 'home' },
  { name: '디깅', icon: 'dig' },
  { name: '커뮤니티', icon: 'community' },
  { name: '내 페이지', icon: 'profile' },
]

const interactiveAreas = '.search-form, .brand, .account-button, .bottom-nav, .results'

export default function App() {
  const [isSearchOpen, setSearchOpen] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [search, setSearch] = useState({ status: 'idle', items: [], query: '' })
  const [notice, setNotice] = useState('')
  const inputRef = useRef(null)
  const pendingRequest = useRef(null)
  const pointerStartedInInteractiveArea = useRef(false)

  async function submitSearch(event) {
    event.preventDefault()
    const query = keyword.trim()
    setSearchOpen(true)
    if (!query) {
      inputRef.current?.focus()
      return
    }

    pendingRequest.current?.abort()
    const request = new AbortController()
    pendingRequest.current = request
    setSearch({ status: 'loading', items: [], query })

    try {
      const response = await fetch(`/api/music/search?keyword=${encodeURIComponent(query)}`, { signal: request.signal })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const items = await response.json()
      if (!Array.isArray(items)) throw new Error('Unexpected response')
      setSearch({ status: 'success', items, query })
    } catch (error) {
      if (error.name !== 'AbortError') setSearch({ status: 'error', items: [], query })
    }
  }

  function resetHome() {
    pendingRequest.current?.abort()
    inputRef.current?.blur()
    setSearchOpen(false)
    setKeyword('')
    setSearch({ status: 'idle', items: [], query: '' })
    setNotice('')
  }

  function showComingSoon(name) {
    setNotice(`${name} 화면은 준비 중입니다.`)
  }

  function handlePageClick(event) {
    const startedInInteractiveArea = pointerStartedInInteractiveArea.current
    pointerStartedInInteractiveArea.current = false
    if (!isSearchOpen) return
    if (startedInInteractiveArea || event.target.closest(interactiveAreas)) return
    resetHome()
  }

  return (
    <main
      className={`home-page ${isSearchOpen ? 'search-open' : ''}`}
      onPointerDownCapture={(event) => { pointerStartedInInteractiveArea.current = Boolean(event.target.closest(interactiveAreas)) }}
      onClick={handlePageClick}
    >
      <Constellation variant="home" />
      <Constellation variant="search" />
      <button className="account-button" type="button" aria-label="내 페이지" onClick={() => showComingSoon('내 페이지')}><Icon name="profile" size={24} /></button>

      <section className="hero" aria-labelledby="brand-title">
        <h1 id="brand-title" className="brand"><button type="button" onClick={resetHome}>DIGGER</button></h1>
        <form className="search-form" role="search" onClick={(event) => { if (event.target === event.currentTarget) inputRef.current?.focus() }} onFocusCapture={() => setSearchOpen(true)} onSubmit={submitSearch}>
          <button className="search-icon" type="submit" aria-label="검색"><Icon name="search" size={32} /></button>
          <input
            ref={inputRef}
            type="search"
            name="keyword"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => { if (event.key === 'Escape') resetHome() }}
            placeholder={isSearchOpen ? '곡, 아티스트, 앨범을 검색해보세요' : '어떤 음악에서 디깅을 시작할까요?'}
            aria-label="음악 검색어"
          />
        </form>
        <p className="tagline">한 곡에서 시작해, 연결을 따라 발견하세요</p>
      </section>

      {isSearchOpen && (
        <section className="search-stage" aria-live="polite">
          {search.status === 'idle' ? (
            <div className="search-empty">
              <div className="empty-icon"><Icon name="search" size={64} /></div>
              <h2>어떤 음악을 찾고 있나요?</h2>
              <p>검색어를 입력하면 결과가 여기에 표시돼요</p>
            </div>
          ) : (
            <div className="results" aria-label="검색 결과">
              <div className="results-heading"><h2>“{search.query}” 검색 결과</h2><button type="button" onClick={resetHome} aria-label="검색 결과 닫기">×</button></div>
              {search.status === 'loading' && <p>음악을 찾고 있어요…</p>}
              {search.status === 'error' && <p>검색 결과를 불러오지 못했습니다. 백엔드 연결을 확인해 주세요.</p>}
              {search.status === 'success' && search.items.length === 0 && <p>검색 결과가 없습니다.</p>}
              {search.status === 'success' && search.items.length > 0 && (
                <ul className="result-list">
                  {search.items.map((item) => <li key={item.id ?? `${item.title}-${item.artist}`}><strong>{item.title}</strong><span>{item.artist}{item.album ? ` · ${item.album}` : ''}</span></li>)}
                </ul>
              )}
            </div>
          )}
        </section>
      )}

      {notice && <div className="notice" role="status">{notice}</div>}

      <nav className="bottom-nav" aria-label="주 메뉴">
        {navItems.map((item) => (
          <button
            key={item.name}
            type="button"
            className={`nav-item ${item.name === '메인' ? 'active' : ''}`}
            aria-current={item.name === '메인' ? 'page' : undefined}
            onClick={() => item.name === '메인' ? resetHome() : showComingSoon(item.name)}
          >
            <Icon name={item.icon} size={30} />
            <span>{item.name}</span>
            {item.name === '메인' && <i className="active-dot" aria-hidden="true" />}
          </button>
        ))}
      </nav>
    </main>
  )
}
