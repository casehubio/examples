import { LitElement, html, css, svg, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { CharacterSnapshot, RoomSnapshot, CHARACTER_COLORS } from './types.js';

const ROOMS = ['entrance-hall', 'kitchen', 'ballroom'] as const;
const ROOM_LABELS: Record<string, string> = {
  'entrance-hall': 'Entrance Hall',
  'kitchen': 'Kitchen',
  'ballroom': 'Ballroom',
};

const CHARACTER_ICONS: Record<string, { emoji: string; initials: string }> = {
  'penelope-pitstop': { emoji: '👒', initials: 'PP' },
  'hooded-claw':      { emoji: '🎭', initials: 'HC' },
  'ant-hill-mob':     { emoji: '🤵', initials: 'AM' },
  'dick-dastardly':   { emoji: '🎩', initials: 'DD' },
  'peter-perfect':    { emoji: '💪', initials: 'PT' },
};

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
    .char-body { stroke: #111; stroke-width: 1.5; transition: cx 0.8s ease-in-out; }
    .char-initials { font-size: 7px; font-weight: 700; text-anchor: middle; fill: #fff; pointer-events: none; }
    .char-emoji { font-size: 14px; text-anchor: middle; pointer-events: none; }
    .char-name { font-size: 7px; text-anchor: middle; fill: #aaa; }
    .object-dot { fill: #665; }
    .object-label { fill: #666; font-size: 6px; text-anchor: middle; }
    .title { fill: #daa520; font-size: 14px; font-weight: 700; text-anchor: middle; font-family: Georgia, serif; letter-spacing: 2px; }
    .subtitle { fill: #666; font-size: 8px; text-anchor: middle; font-style: italic; }
    .floor-line { stroke: #444; stroke-width: 2; }
  `;

  render() {
    const w = 720, h = 220;
    const roomW = w / 3;
    const roomY = 50, roomH = 150;

    return html`
      <svg viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg">
        ${svg`<text x="${w / 2}" y="18" class="title">DOILY MANOR</text>`}
        ${svg`<text x="${w / 2}" y="32" class="subtitle">A Most EXTRAORDINARY Evening</text>`}
        ${svg`<line x1="4" y1="${roomY + roomH + 2}" x2="${w - 4}" y2="${roomY + roomH + 2}" class="floor-line" />`}

        ${ROOMS.map((roomId, i) => {
          const rx = i * roomW + 4;
          const rw = roomW - 8;
          const isActive = this.activeRoom === roomId;
          return svg`
            <rect x="${rx}" y="${roomY}" width="${rw}" height="${roomH}"
                  class="room-bg ${isActive ? 'active' : ''}" />
            <text x="${rx + rw / 2}" y="${roomY + 14}" class="room-label">
              ${ROOM_LABELS[roomId]}
            </text>
            ${i < ROOMS.length - 1 ? svg`
              <rect x="${rx + rw - 2}" y="${roomY + roomH / 2 - 15}" width="12" height="30" class="door" />
              <text x="${rx + rw + 4}" y="${roomY + roomH / 2 + 4}" class="door-arrow">⇔</text>
            ` : nothing}
            ${this.renderObjects(roomId, rx, roomY, rw)}
          `;
        })}

        ${this.characters.filter(c => c.active).map(c => {
          const roomIdx = ROOMS.indexOf(c.room as typeof ROOMS[number]);
          if (roomIdx < 0) return nothing;
          const rx = roomIdx * roomW + 4;
          const rw = roomW - 8;
          const cx = rx + c.x * (rw - 30) + 15;
          const cy = roomY + roomH - 35;
          const color = CHARACTER_COLORS[c.id] || '#888';
          const icon = CHARACTER_ICONS[c.id] || { emoji: '\u{1F464}', initials: '??' };

          return svg`
            <circle cx="${cx}" cy="${cy}" r="11" class="char-body" fill="${color}" />
            <text x="${cx}" y="${cy + 3}" class="char-initials">${icon.initials}</text>
            <text x="${cx}" y="${cy - 16}" class="char-emoji">${icon.emoji}</text>
            <text x="${cx}" y="${cy + 22}" class="char-name">${c.name.split(' ')[0]}</text>
          `;
        })}
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
        <text x="${ox}" y="${oy + 12}" class="object-label">${obj.name.substring(0, 10)}</text>
      `;
    });
  }
}
