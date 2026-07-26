import { LitElement, html, css, svg, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { CharacterSnapshot, RoomSnapshot, CHARACTER_COLORS, CHARACTER_SHORT_NAMES } from './types.js';

const ROOMS = ['entrance-hall', 'kitchen', 'ballroom'] as const;
const ROOM_LABELS: Record<string, string> = {
  'entrance-hall': 'Entrance Hall',
  'kitchen': 'Kitchen',
  'ballroom': 'Ballroom',
};

@customElement('manor-view')
export class ManorView extends LitElement {
  @property({ type: Array }) characters: CharacterSnapshot[] = [];
  @property({ type: Array }) rooms: RoomSnapshot[] = [];
  @property({ type: String }) activeRoom: string | null = null;

  static styles = css`
    :host { display: block; width: 100%; }
    svg { width: 100%; height: 100%; }
    .room-bg { fill: #2a2a3e; stroke: #444; stroke-width: 1.5; rx: 4; }
    .room-bg.active { fill: #2a2a4e; stroke: #667; }
    .room-label { fill: #aaa; font-size: 11px; font-weight: 600; text-anchor: middle; }
    .room-divider { stroke: #555; stroke-width: 1; stroke-dasharray: 4 2; }
    .door { fill: #665544; rx: 2; }
    .door-label { fill: #888; font-size: 7px; text-anchor: middle; }
    .character-dot { stroke: #111; stroke-width: 1.5; transition: cx 0.8s ease-in-out; }
    .character-label { font-size: 8px; text-anchor: middle; fill: #ddd; transition: x 0.8s ease-in-out; }
    .object-icon { fill: #888; font-size: 10px; text-anchor: middle; }
    .title { fill: #daa520; font-size: 14px; font-weight: 700; text-anchor: middle; font-family: Georgia, serif; letter-spacing: 2px; }
    .subtitle { fill: #777; font-size: 8px; text-anchor: middle; font-style: italic; }
  `;

  render() {
    const w = 720, h = 200;
    const roomW = w / 3;
    const roomY = 50, roomH = 130;

    return html`
      <svg viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg">
        <!-- Title -->
        ${svg`<text x="${w / 2}" y="20" class="title">DOILY MANOR</text>`}
        ${svg`<text x="${w / 2}" y="34" class="subtitle">A Most EXTRAORDINARY Evening</text>`}

        <!-- Rooms -->
        ${ROOMS.map((roomId, i) => {
          const rx = i * roomW;
          const isActive = this.activeRoom === roomId;
          return svg`
            <rect x="${rx + 2}" y="${roomY}" width="${roomW - 4}" height="${roomH}"
                  class="room-bg ${isActive ? 'active' : ''}" />
            <text x="${rx + roomW / 2}" y="${roomY + 16}" class="room-label">
              ${ROOM_LABELS[roomId]}
            </text>

            <!-- Door indicators between rooms -->
            ${i < ROOMS.length - 1 ? svg`
              <rect x="${rx + roomW - 6}" y="${roomY + roomH / 2 - 12}" width="12" height="24" class="door" />
              <text x="${rx + roomW}" y="${roomY + roomH / 2 + 16}" class="door-label">↔</text>
            ` : nothing}

            <!-- Objects in room -->
            ${this.renderObjects(roomId, rx, roomY, roomW, roomH)}
          `;
        })}

        <!-- Characters -->
        ${this.characters.filter(c => c.active).map(c => {
          const roomIdx = ROOMS.indexOf(c.room as typeof ROOMS[number]);
          if (roomIdx < 0) return nothing;
          const rx = roomIdx * roomW;
          const cx = rx + c.x * (roomW - 20) + 10;
          const cy = roomY + roomH - 30;
          const color = CHARACTER_COLORS[c.id] || '#888';
          const label = CHARACTER_SHORT_NAMES[c.id] || c.id;

          return svg`
            <circle cx="${cx}" cy="${cy}" r="8" class="character-dot" fill="${color}" />
            <text x="${cx}" y="${cy + 18}" class="character-label">${label}</text>
          `;
        })}
      </svg>
    `;
  }

  private renderObjects(roomId: string, rx: number, roomY: number, roomW: number, _roomH: number) {
    const room = this.rooms.find(r => r.id === roomId);
    if (!room) return nothing;
    return room.objects.map(obj => {
      const ox = rx + obj.x * (roomW - 20) + 10;
      const oy = roomY + 40;
      return svg`<text x="${ox}" y="${oy}" class="object-icon">•</text>`;
    });
  }
}
