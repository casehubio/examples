import { LitElement, html, css, svg, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { CharacterSnapshot, RoomSnapshot } from './types.js';

interface RoomLayout { row: number; col: number; }

const ROOM_GRID: Record<string, RoomLayout> = {
  'entrance-hall': { row: 0, col: 0 },
  'kitchen':       { row: 0, col: 1 },
  'ballroom':      { row: 0, col: 2 },
  'library':       { row: 1, col: 0 },
  'laboratory':    { row: 1, col: 1 },
  'cellar':        { row: 1, col: 2 },
};

const ROOM_LABELS: Record<string, string> = {
  'entrance-hall': 'Entrance Hall',
  'kitchen':       'Kitchen',
  'ballroom':      'Ballroom',
  'library':       'Library',
  'laboratory':    'Laboratory',
  'cellar':        'Cellar',
};

const ADJACENCIES: [string, string][] = [
  ['entrance-hall', 'kitchen'],
  ['kitchen', 'ballroom'],
  ['entrance-hall', 'library'],
  ['library', 'laboratory'],
  ['laboratory', 'cellar'],
];

const CHARACTER_LABELS: Record<string, string> = {
  'penelope-pitstop': 'Penelope',
  'hooded-claw':      'Sneekly',
  'ant-hill-mob':     'Ant Hill Mob',
  'dick-dastardly':   'Dastardly',
  'peter-perfect':    'Peter',
  'muttley':          'Muttley',
  'pat-pending':      'Pat Pending',
  'sergeant-blast':   'Sgt. Blast',
  'private-meekly':   'Pvt. Meekly',
  'lazy-luke':        'Lazy Luke',
  'blubber-bear':     'Blubber Bear',
  'rock-slag':        'Rock',
  'gravel-slag':      'Gravel',
  'rufus-ruffcut':    'Rufus',
  'sawtooth':         'Sawtooth',
  'big-gruesome':     'Big Gruesome',
  'little-gruesome':  'Lil Gruesome',
};

const OVERLAP_THRESHOLD = 30;

function renderCharacterAtOrigin(id: string) {
  const s = 0.55;
  const ox = -12 * s;
  const oy = -28 * s;

  switch (id) {
    case 'penelope-pitstop':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="10" rx="9" ry="10" fill="#F5D76E"/>
        <ellipse cx="4" cy="14" rx="4" ry="7" fill="#F5D76E"/>
        <ellipse cx="20" cy="14" rx="4" ry="7" fill="#F5D76E"/>
        <circle cx="12" cy="9" r="7" fill="#FDDCB5"/>
        <ellipse cx="9.5" cy="8" rx="1.8" ry="2" fill="white"/>
        <ellipse cx="14.5" cy="8" rx="1.8" ry="2" fill="white"/>
        <circle cx="10" cy="8.3" r="1" fill="#4488CC"/>
        <circle cx="15" cy="8.3" r="1" fill="#4488CC"/>
        <path d="M7.5 6.5 L7 5" stroke="#333" stroke-width="0.6" fill="none"/>
        <path d="M16.5 6.5 L17 5" stroke="#333" stroke-width="0.6" fill="none"/>
        <path d="M10 12 Q12 14 14 12" stroke="#CC5555" stroke-width="0.7" fill="none"/>
        <ellipse cx="12" cy="3" rx="9" ry="4" fill="#FF69B4"/>
        <rect x="5" y="2" width="14" height="2.5" rx="1" fill="#FF1493"/>
        <path d="M7 16 L5 35 L19 35 L17 16 Z" fill="#FF69B4"/>
        <rect x="6" y="22" width="12" height="1.5" fill="#FF1493" rx="0.5"/>
        <circle cx="12" cy="22.5" r="1" fill="#FFD700"/>
        <path d="M7 18 Q3 24 4 28" stroke="#FDDCB5" stroke-width="2" fill="none" stroke-linecap="round"/>
        <path d="M17 18 Q21 24 20 28" stroke="#FDDCB5" stroke-width="2" fill="none" stroke-linecap="round"/>
        <rect x="6" y="35" width="4" height="3" rx="1" fill="#FF1493"/>
        <rect x="14" y="35" width="4" height="3" rx="1" fill="#FF1493"/>
      </g>`;

    case 'hooded-claw':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="7" rx="7" ry="8" fill="#333"/>
        <ellipse cx="12" cy="9" rx="6" ry="7" fill="#E8D5B7"/>
        <ellipse cx="9.5" cy="8" rx="1.5" ry="1" fill="white"/>
        <ellipse cx="14.5" cy="8" rx="1.5" ry="1" fill="white"/>
        <circle cx="10" cy="8" r="0.7" fill="#2a4a2a"/>
        <circle cx="15" cy="8" r="0.7" fill="#2a4a2a"/>
        <path d="M8 6.5 Q9.5 5.5 11 6.5" stroke="#333" stroke-width="0.6" fill="none"/>
        <path d="M13 6.5 Q14.5 5.5 16 6.5" stroke="#333" stroke-width="0.6" fill="none"/>
        <path d="M9 12 Q12 13 15 12" stroke="#555" stroke-width="0.5" fill="none"/>
        <path d="M6 16 L4 35 L20 35 L18 16 Z" fill="#2d5a27"/>
        <path d="M10 16 L8 24 L12 24 Z" fill="#1a3a15"/>
        <path d="M14 16 L16 24 L12 24 Z" fill="#1a3a15"/>
        <path d="M11.5 16 L11 26 L13 26 L12.5 16 Z" fill="#8B0000"/>
        <path d="M6 18 Q2 24 3 28" stroke="#2d5a27" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <path d="M18 18 Q22 24 21 28" stroke="#2d5a27" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <circle cx="3" cy="28" r="1.5" fill="#E8D5B7"/>
        <circle cx="21" cy="28" r="1.5" fill="#E8D5B7"/>
        <rect x="5" y="35" width="5" height="3" rx="1" fill="#222"/>
        <rect x="14" y="35" width="5" height="3" rx="1" fill="#222"/>
      </g>`;

    case 'ant-hill-mob':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <circle cx="4" cy="10" r="3.5" fill="#C4A882"/>
        <rect x="0" y="6" width="8" height="3" rx="1.5" fill="#4a3728"/>
        <circle cx="3" cy="10" r="0.7" fill="white"/>
        <circle cx="5" cy="10" r="0.7" fill="white"/>
        <path d="M1 13 L0 30 L8 30 L7 13 Z" fill="#5a3a1e"/>
        <circle cx="20" cy="10" r="3.5" fill="#C4A882"/>
        <rect x="16" y="6" width="8" height="3" rx="1.5" fill="#4a3728"/>
        <circle cx="19" cy="10" r="0.7" fill="white"/>
        <circle cx="21" cy="10" r="0.7" fill="white"/>
        <path d="M17 13 L16 30 L24 30 L23 13 Z" fill="#5a3a1e"/>
        <circle cx="12" cy="8" r="5" fill="#D2B48C"/>
        <rect x="5" y="3" width="14" height="4" rx="2" fill="#4a3728"/>
        <rect x="6" y="5" width="12" height="2" fill="#333"/>
        <ellipse cx="10" cy="8" rx="1.2" ry="1.4" fill="white"/>
        <ellipse cx="14" cy="8" rx="1.2" ry="1.4" fill="white"/>
        <circle cx="10.3" cy="8.2" r="0.7" fill="#333"/>
        <circle cx="14.3" cy="8.2" r="0.7" fill="#333"/>
        <path d="M10 11 Q12 12.5 14 11" stroke="#555" stroke-width="0.5" fill="none"/>
        <path d="M6 13 L4 35 L20 35 L18 13 Z" fill="#6B4226"/>
        <rect x="7" y="18" width="10" height="1" fill="#444" rx="0.5"/>
      </g>`;

    case 'dick-dastardly':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <rect x="6" y="-2" width="12" height="8" rx="1" fill="#2a0a3a"/>
        <ellipse cx="12" cy="6" rx="9" ry="2.5" fill="#2a0a3a"/>
        <rect x="7" y="3" width="10" height="1" fill="#6a0dad"/>
        <ellipse cx="12" cy="10" rx="6" ry="5.5" fill="#E8D5B7"/>
        <ellipse cx="9.5" cy="9" rx="1.5" ry="1.2" fill="white"/>
        <ellipse cx="14.5" cy="9" rx="1.5" ry="1.2" fill="white"/>
        <circle cx="10" cy="9.2" r="0.8" fill="#333"/>
        <circle cx="15" cy="9.2" r="0.8" fill="#333"/>
        <path d="M8 12 Q6 11 4 12" stroke="#333" stroke-width="1" fill="none"/>
        <path d="M16 12 Q18 11 20 12" stroke="#333" stroke-width="1" fill="none"/>
        <path d="M8 12 L16 12" stroke="#333" stroke-width="0.5"/>
        <path d="M10 13.5 Q12 14.5 14 13.5" stroke="#555" stroke-width="0.5" fill="none"/>
        <path d="M6 16 L3 35 L21 35 L18 16 Z" fill="#6a0dad"/>
        <path d="M6 16 L8 13 L12 16" fill="#5a0a9d"/>
        <path d="M18 16 L16 13 L12 16" fill="#5a0a9d"/>
        <path d="M6 18 Q1 24 2 28" stroke="#6a0dad" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <path d="M18 18 Q23 24 22 28" stroke="#6a0dad" stroke-width="2.5" fill="none" stroke-linecap="round"/>
        <circle cx="2" cy="28" r="1.5" fill="#444"/>
        <circle cx="22" cy="28" r="1.5" fill="#444"/>
        <rect x="5" y="35" width="5" height="3" rx="1" fill="#333"/>
        <rect x="14" y="35" width="5" height="3" rx="1" fill="#333"/>
      </g>`;

    case 'peter-perfect':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="6" rx="7" ry="7" fill="#C4944A"/>
        <path d="M5 9 Q5 4 12 4 Q19 4 19 9 L18 14 Q12 16 6 14 Z" fill="#FDDCB5"/>
        <ellipse cx="9" cy="8" rx="2" ry="2" fill="white"/>
        <ellipse cx="15" cy="8" rx="2" ry="2" fill="white"/>
        <circle cx="9.5" cy="8.3" r="1" fill="#4169E1"/>
        <circle cx="15.5" cy="8.3" r="1" fill="#4169E1"/>
        <path d="M9 12 Q12 14.5 15 12" stroke="#CC5555" stroke-width="0.7" fill="none"/>
        <path d="M6 16 L4 35 L20 35 L18 16 Z" fill="#4169E1"/>
        <rect x="11" y="16" width="2" height="19" fill="#FFD700"/>
        <path d="M7 14 Q12 17 17 14" stroke="white" stroke-width="2" fill="none"/>
        <path d="M17 14 Q20 18 18 22" stroke="white" stroke-width="1.5" fill="none"/>
        <path d="M6 18 Q2 23 3 28" stroke="#4169E1" stroke-width="3" fill="none" stroke-linecap="round"/>
        <path d="M18 18 Q22 23 21 28" stroke="#4169E1" stroke-width="3" fill="none" stroke-linecap="round"/>
        <circle cx="3" cy="28" r="1.5" fill="#FDDCB5"/>
        <circle cx="21" cy="28" r="1.5" fill="#FDDCB5"/>
        <rect x="5" y="35" width="5" height="3" rx="1" fill="#333"/>
        <rect x="14" y="35" width="5" height="3" rx="1" fill="#333"/>
      </g>`;

    default:
      return svg`<circle r="8" fill="#888" stroke="#111" stroke-width="1"/>`;
  }
}

interface CharPos { id: string; name: string; absX: number; baseY: number; labelRow: number; }

function layoutCharacters(
  characters: CharacterSnapshot[], roomW: number, roomH: number, rowGap: number, topY: number
): CharPos[] {
  const minSpacing = OVERLAP_THRESHOLD;
  const positions: CharPos[] = [];

  const byRoom = new Map<string, { c: CharacterSnapshot; origX: number }[]>();
  for (const c of characters.filter(c => c.active)) {
    if (!ROOM_GRID[c.room]) continue;
    if (!byRoom.has(c.room)) byRoom.set(c.room, []);
    byRoom.get(c.room)!.push({ c, origX: c.x });
  }

  for (const [roomId, chars] of byRoom) {
    const layout = ROOM_GRID[roomId];
    const rx = layout.col * roomW + 4;
    const rw = roomW - 8;
    const roomY = topY + layout.row * (roomH + rowGap);
    const baseY = roomY + roomH - 15;
    const margin = 15;

    chars.sort((a, b) => a.origX - b.origX);

    const displayXs: number[] = chars.map(ch => rx + ch.origX * (rw - 2 * margin) + margin);

    for (let i = 1; i < displayXs.length; i++) {
      if (displayXs[i] - displayXs[i - 1] < minSpacing) {
        displayXs[i] = displayXs[i - 1] + minSpacing;
      }
    }

    const maxX = rx + rw - margin;
    if (displayXs.length > 0 && displayXs[displayXs.length - 1] > maxX) {
      const overflow = displayXs[displayXs.length - 1] - maxX;
      for (let i = 0; i < displayXs.length; i++) {
        displayXs[i] -= overflow * (i + 1) / displayXs.length;
      }
    }

    for (let i = 0; i < chars.length; i++) {
      let labelRow = 0;
      for (let j = 0; j < i; j++) {
        if (Math.abs(displayXs[i] - positions[positions.length - (i - j)].absX) < minSpacing) {
          labelRow = Math.max(labelRow, positions[positions.length - (i - j)].labelRow + 1);
        }
      }
      positions.push({
        id: chars[i].c.id,
        name: chars[i].c.name,
        absX: displayXs[i],
        baseY,
        labelRow,
      });
    }
  }

  return positions;
}

@customElement('manor-view')
export class ManorView extends LitElement {
  @property({ type: Array }) characters: CharacterSnapshot[] = [];
  @property({ type: Array }) rooms: RoomSnapshot[] = [];
  @property({ type: String }) activeRoom: string | null = null;

  static styles = css`
    :host { display: block; width: 100%; }
    svg { width: 100%; height: 100%; }
    .room-bg { fill: #2a2a3e; stroke: #444; stroke-width: 1.5; rx: 6; }
    .room-bg.active { fill: #2a2a4e; stroke: #667; }
    .room-label { fill: #999; font-size: 10px; font-weight: 600; text-anchor: middle; text-transform: uppercase; letter-spacing: 1px; }
    .door { fill: #554433; rx: 2; }
    .door-arrow { fill: #776655; font-size: 10px; text-anchor: middle; }
    .passage-line { stroke: #554433; stroke-width: 2; stroke-dasharray: 4 3; }
    .char-group { transition: transform 1.5s ease-in-out; }
    .char-name { font-size: 6px; text-anchor: middle; fill: #aaa; }
    .object-dot { fill: #665; }
    .object-label { fill: #555; font-size: 5.5px; text-anchor: middle; }
    .title { fill: #daa520; font-size: 14px; font-weight: 700; text-anchor: middle; font-family: Georgia, serif; letter-spacing: 2px; }
    .subtitle { fill: #666; font-size: 8px; text-anchor: middle; font-style: italic; }
    .floor-line { stroke: #444; stroke-width: 2; }
  `;

  render() {
    const w = 720, roomH = 150, rowGap = 30, topY = 50;
    const h = topY + 2 * roomH + rowGap + 20;
    const roomW = w / 3;

    const charPositions = layoutCharacters(this.characters, roomW, roomH, rowGap, topY);
    const roomIds = Object.keys(ROOM_GRID);

    return html`
      <svg viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg">
        ${svg`<text x="${w / 2}" y="18" class="title">DOILY MANOR</text>`}
        ${svg`<text x="${w / 2}" y="32" class="subtitle">A Most EXTRAORDINARY Evening</text>`}

        ${svg`<line x1="4" y1="${topY + roomH + 2}" x2="${w - 4}" y2="${topY + roomH + 2}" class="floor-line" />`}
        ${svg`<line x1="4" y1="${topY + 2 * roomH + rowGap + 2}" x2="${w - 4}" y2="${topY + 2 * roomH + rowGap + 2}" class="floor-line" />`}

        ${roomIds.map(roomId => {
          const layout = ROOM_GRID[roomId];
          const rx = layout.col * roomW + 4;
          const rw = roomW - 8;
          const ry = topY + layout.row * (roomH + rowGap);
          const isActive = this.activeRoom === roomId;
          return svg`
            <rect x="${rx}" y="${ry}" width="${rw}" height="${roomH}"
                  class="room-bg ${isActive ? 'active' : ''}" />
            <text x="${rx + rw / 2}" y="${ry + 14}" class="room-label">
              ${ROOM_LABELS[roomId]}
            </text>
            ${this.renderObjects(roomId, rx, ry, rw)}
          `;
        })}

        ${ADJACENCIES.map(([a, b]) => {
          const la = ROOM_GRID[a], lb = ROOM_GRID[b];
          const ax = la.col * roomW + 4, ay = topY + la.row * (roomH + rowGap);
          const bx = lb.col * roomW + 4, by = topY + lb.row * (roomH + rowGap);
          const aw = roomW - 8;

          if (la.row === lb.row) {
            const doorX = Math.min(ax, bx) + aw - 2;
            const doorY = ay + roomH / 2 - 15;
            return svg`
              <rect x="${doorX}" y="${doorY}" width="12" height="30" class="door" />
              <text x="${doorX + 6}" y="${doorY + 19}" class="door-arrow">⇔</text>
            `;
          } else {
            const cx = Math.min(ax, bx) + aw / 2;
            const topBot = Math.min(ay, by) + roomH;
            const botTop = Math.max(ay, by);
            return svg`
              <line x1="${cx}" y1="${topBot}" x2="${cx}" y2="${botTop}" class="passage-line" />
              <text x="${cx}" y="${topBot + (botTop - topBot) / 2 + 4}" class="door-arrow">⇕</text>
            `;
          }
        })}

        ${charPositions.map(p => svg`
          <g class="char-group" style="transform: translate(${p.absX}px, ${p.baseY}px)">
            ${renderCharacterAtOrigin(p.id)}
            <text x="0" y="${12 + p.labelRow * 8}" class="char-name">${CHARACTER_LABELS[p.id] || p.name}</text>
          </g>
        `)}
      </svg>
    `;
  }

  private renderObjects(roomId: string, rx: number, roomY: number, roomW: number) {
    const room = this.rooms.find(r => r.id === roomId);
    if (!room) return nothing;
    return room.objects.map(obj => {
      const ox = rx + obj.x * (roomW - 20) + 10;
      const oy = roomY + 30;
      return svg`
        <circle cx="${ox}" cy="${oy}" r="3" class="object-dot" />
        <text x="${ox}" y="${oy + 12}" class="object-label">${obj.name.substring(0, 12)}</text>
      `;
    });
  }
}
