import { LitElement, html, css, svg, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { CharacterSnapshot, RoomSnapshot } from './types.js';
import { ROOM_GRID, layoutCharacters } from './layout.js';

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

    case 'muttley':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="14" rx="8" ry="6" fill="#8B6914"/>
        <circle cx="12" cy="8" r="6" fill="#A0781E"/>
        <ellipse cx="8" cy="5" rx="3" ry="5" fill="#8B6914"/>
        <ellipse cx="16" cy="5" rx="3" ry="5" fill="#8B6914"/>
        <circle cx="9" cy="8" r="1.2" fill="white"/><circle cx="9.5" cy="8" r="0.6" fill="#333"/>
        <circle cx="15" cy="8" r="1.2" fill="white"/><circle cx="15.5" cy="8" r="0.6" fill="#333"/>
        <ellipse cx="12" cy="11" rx="2.5" ry="1.5" fill="#333"/>
        <path d="M10 13 Q12 15 14 13" stroke="#333" stroke-width="0.5" fill="none"/>
        <path d="M5 20 L4 35 L9 35 L8 20 Z" fill="#8B6914"/>
        <path d="M15 20 L16 35 L20 35 L19 20 Z" fill="#8B6914"/>
        <path d="M18 14 Q22 12 20 8" stroke="#8B6914" stroke-width="2" fill="none"/>
      </g>`;

    case 'pat-pending':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <circle cx="12" cy="9" r="6.5" fill="#FDDCB5"/>
        <rect x="4" y="2" width="16" height="5" rx="2" fill="white"/>
        <rect x="6" y="0" width="12" height="3" rx="1.5" fill="#ddd"/>
        <circle cx="8" cy="4" r="2.5" fill="#87CEEB" stroke="#666" stroke-width="0.5"/>
        <circle cx="16" cy="4" r="2.5" fill="#87CEEB" stroke="#666" stroke-width="0.5"/>
        <ellipse cx="9.5" cy="9" rx="1.5" ry="1.5" fill="white"/>
        <ellipse cx="14.5" cy="9" rx="1.5" ry="1.5" fill="white"/>
        <circle cx="10" cy="9.2" r="0.7" fill="#2E8B57"/>
        <circle cx="15" cy="9.2" r="0.7" fill="#2E8B57"/>
        <path d="M10 12 Q12 13.5 14 12" stroke="#555" stroke-width="0.5" fill="none"/>
        <path d="M6 16 L4 35 L20 35 L18 16 Z" fill="white"/>
        <rect x="7" y="20" width="10" height="1" fill="#ddd"/>
        <rect x="5" y="35" width="5" height="3" rx="1" fill="#555"/>
        <rect x="14" y="35" width="5" height="3" rx="1" fill="#555"/>
      </g>`;

    case 'sergeant-blast':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <rect x="5" y="0" width="14" height="6" rx="1" fill="#556B2F"/>
        <rect x="8" y="4" width="8" height="2" fill="#333"/>
        <ellipse cx="12" cy="3" rx="3" ry="1" fill="#DAA520"/>
        <circle cx="12" cy="10" r="6" fill="#FDDCB5"/>
        <ellipse cx="9.5" cy="9" rx="1.3" ry="1.5" fill="white"/>
        <ellipse cx="14.5" cy="9" rx="1.3" ry="1.5" fill="white"/>
        <circle cx="10" cy="9.3" r="0.7" fill="#333"/>
        <circle cx="15" cy="9.3" r="0.7" fill="#333"/>
        <path d="M8 7 L11 7.5" stroke="#333" stroke-width="0.8"/>
        <path d="M16 7 L13 7.5" stroke="#333" stroke-width="0.8"/>
        <path d="M9 13 L15 13" stroke="#8B4513" stroke-width="1.5"/>
        <path d="M6 16 L4 35 L20 35 L18 16 Z" fill="#556B2F"/>
        <rect x="9" y="18" width="6" height="1" fill="#DAA520"/>
        <rect x="5" y="35" width="5" height="3" rx="1" fill="#333"/>
        <rect x="14" y="35" width="5" height="3" rx="1" fill="#333"/>
      </g>`;

    case 'private-meekly':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <rect x="7" y="2" width="10" height="5" rx="1" fill="#6B8E23"/>
        <rect x="9" y="5" width="6" height="1.5" fill="#333"/>
        <circle cx="12" cy="11" r="5" fill="#FDDCB5"/>
        <ellipse cx="10" cy="10.5" rx="1.2" ry="1.5" fill="white"/>
        <ellipse cx="14" cy="10.5" rx="1.2" ry="1.5" fill="white"/>
        <circle cx="10.3" cy="10.8" r="0.6" fill="#333"/>
        <circle cx="14.3" cy="10.8" r="0.6" fill="#333"/>
        <path d="M10 13 Q12 14 14 13" stroke="#555" stroke-width="0.4" fill="none"/>
        <path d="M7 17 L6 32 L18 32 L17 17 Z" fill="#6B8E23"/>
        <rect x="5" y="32" width="4" height="2.5" rx="1" fill="#333"/>
        <rect x="14" y="32" width="4" height="2.5" rx="1" fill="#333"/>
      </g>`;

    case 'lazy-luke':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="4" rx="9" ry="4" fill="#DAA520"/>
        <rect x="5" y="3" width="14" height="3" fill="#C4944A"/>
        <circle cx="12" cy="10" r="6" fill="#FDDCB5"/>
        <ellipse cx="9.5" cy="9" rx="1.5" ry="1.2" fill="white"/>
        <ellipse cx="14.5" cy="9" rx="1.5" ry="1.2" fill="white"/>
        <circle cx="10" cy="9" r="0.6" fill="#333"/>
        <circle cx="15" cy="9" r="0.6" fill="#333"/>
        <path d="M9 12.5 Q12 14 15 12.5" stroke="#555" stroke-width="0.5" fill="none"/>
        <path d="M6 16 L3 40 L21 40 L18 16 Z" fill="#8B7355"/>
        <path d="M10 16 L8 24 L16 24 L14 16 Z" fill="#DAA520"/>
        <rect x="5" y="40" width="5" height="3" rx="1" fill="#654321"/>
        <rect x="14" y="40" width="5" height="3" rx="1" fill="#654321"/>
      </g>`;

    case 'blubber-bear':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="18" rx="10" ry="14" fill="#8B4513"/>
        <circle cx="12" cy="8" r="7" fill="#A0522D"/>
        <circle cx="6" cy="3" r="3" fill="#8B4513"/>
        <circle cx="18" cy="3" r="3" fill="#8B4513"/>
        <circle cx="6" cy="3" r="1.5" fill="#D2B48C"/>
        <circle cx="18" cy="3" r="1.5" fill="#D2B48C"/>
        <ellipse cx="12" cy="12" rx="5" ry="4" fill="#D2B48C"/>
        <circle cx="9" cy="8" r="1.5" fill="white"/><circle cx="9.5" cy="8" r="0.8" fill="#333"/>
        <circle cx="15" cy="8" r="1.5" fill="white"/><circle cx="15.5" cy="8" r="0.8" fill="#333"/>
        <ellipse cx="12" cy="11" rx="2" ry="1.2" fill="#333"/>
        <path d="M10 14 Q12 15.5 14 14" stroke="#555" stroke-width="0.5" fill="none"/>
        <path d="M4 30 L3 35 L8 35 L7 30 Z" fill="#8B4513"/>
        <path d="M17 30 L18 35 L21 35 L20 30 Z" fill="#8B4513"/>
      </g>`;

    case 'rock-slag':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="20" rx="9" ry="12" fill="#A0522D"/>
        <circle cx="12" cy="9" r="7" fill="#FDDCB5"/>
        <rect x="4" y="2" width="16" height="5" rx="2" fill="#A0522D"/>
        <ellipse cx="9" cy="9" rx="2" ry="1.8" fill="white"/>
        <ellipse cx="15" cy="9" rx="2" ry="1.8" fill="white"/>
        <circle cx="9.5" cy="9" r="1" fill="#333"/>
        <circle cx="15.5" cy="9" r="1" fill="#333"/>
        <path d="M7 5 L6 4" stroke="#333" stroke-width="1"/>
        <path d="M17 5 L18 4" stroke="#333" stroke-width="1"/>
        <path d="M9 13 Q12 15 15 13" stroke="#555" stroke-width="0.7" fill="none"/>
        <rect x="4" y="32" width="5" height="4" rx="1" fill="#8B4513"/>
        <rect x="15" y="32" width="5" height="4" rx="1" fill="#8B4513"/>
      </g>`;

    case 'gravel-slag':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="20" rx="9" ry="12" fill="#708090"/>
        <circle cx="12" cy="9" r="7" fill="#FDDCB5"/>
        <rect x="4" y="2" width="16" height="5" rx="2" fill="#708090"/>
        <ellipse cx="9" cy="9" rx="2" ry="1.8" fill="white"/>
        <ellipse cx="15" cy="9" rx="2" ry="1.8" fill="white"/>
        <circle cx="9.5" cy="9" r="1" fill="#333"/>
        <circle cx="15.5" cy="9" r="1" fill="#333"/>
        <path d="M7 5 L6 4" stroke="#333" stroke-width="1"/>
        <path d="M17 5 L18 4" stroke="#333" stroke-width="1"/>
        <path d="M9 13 Q12 15 15 13" stroke="#555" stroke-width="0.7" fill="none"/>
        <rect x="4" y="32" width="5" height="4" rx="1" fill="#555"/>
        <rect x="15" y="32" width="5" height="4" rx="1" fill="#555"/>
      </g>`;

    case 'rufus-ruffcut':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <circle cx="12" cy="9" r="7" fill="#FDDCB5"/>
        <path d="M5 6 Q12 2 19 6" fill="#B22222"/>
        <path d="M6 10 Q4 14 6 16" stroke="#8B4513" stroke-width="2" fill="none"/>
        <path d="M18 10 Q20 14 18 16" stroke="#8B4513" stroke-width="2" fill="none"/>
        <ellipse cx="9.5" cy="9" rx="1.5" ry="1.5" fill="white"/>
        <ellipse cx="14.5" cy="9" rx="1.5" ry="1.5" fill="white"/>
        <circle cx="10" cy="9" r="0.7" fill="#333"/>
        <circle cx="15" cy="9" r="0.7" fill="#333"/>
        <path d="M8 13 Q12 16 16 13" fill="#8B4513"/>
        <path d="M6 16 L4 35 L20 35 L18 16 Z" fill="#B22222"/>
        <rect x="8" y="16" width="8" height="4" fill="#228B22"/>
        <rect x="5" y="35" width="5" height="3" rx="1" fill="#654321"/>
        <rect x="14" y="35" width="5" height="3" rx="1" fill="#654321"/>
      </g>`;

    case 'sawtooth':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="16" rx="8" ry="10" fill="#D2691E"/>
        <circle cx="12" cy="8" r="6" fill="#D2691E"/>
        <ellipse cx="12" cy="12" rx="4" ry="3" fill="#F5DEB3"/>
        <circle cx="9" cy="7" r="1.5" fill="white"/><circle cx="9.5" cy="7" r="0.7" fill="#333"/>
        <circle cx="15" cy="7" r="1.5" fill="white"/><circle cx="15.5" cy="7" r="0.7" fill="#333"/>
        <rect x="9" y="11" width="6" height="2" rx="0.5" fill="white"/>
        <line x1="10" y1="11" x2="10" y2="13" stroke="#D2691E" stroke-width="0.5"/>
        <line x1="12" y1="11" x2="12" y2="13" stroke="#D2691E" stroke-width="0.5"/>
        <line x1="14" y1="11" x2="14" y2="13" stroke="#D2691E" stroke-width="0.5"/>
        <path d="M6 26 L4 30 L20 30 L18 26 Z" fill="#D2691E"/>
        <ellipse cx="12" cy="32" rx="8" ry="3" fill="#8B4513"/>
      </g>`;

    case 'big-gruesome':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <ellipse cx="12" cy="20" rx="11" ry="16" fill="#9370DB"/>
        <circle cx="12" cy="7" r="8" fill="#9370DB"/>
        <circle cx="6" cy="1" r="2.5" fill="#7B68EE"/>
        <circle cx="18" cy="1" r="2.5" fill="#7B68EE"/>
        <ellipse cx="9" cy="7" rx="2.5" ry="2" fill="white"/>
        <ellipse cx="15" cy="7" rx="2.5" ry="2" fill="white"/>
        <circle cx="9.5" cy="7" r="1.2" fill="#333"/>
        <circle cx="15.5" cy="7" r="1.2" fill="#333"/>
        <path d="M8 12 Q12 15 16 12" stroke="#333" stroke-width="0.7" fill="none"/>
        <path d="M3 18 Q0 24 2 28" stroke="#9370DB" stroke-width="3" fill="none" stroke-linecap="round"/>
        <path d="M21 18 Q24 24 22 28" stroke="#9370DB" stroke-width="3" fill="none" stroke-linecap="round"/>
        <rect x="4" y="34" width="6" height="4" rx="2" fill="#7B68EE"/>
        <rect x="14" y="34" width="6" height="4" rx="2" fill="#7B68EE"/>
      </g>`;

    case 'little-gruesome':
      return svg`<g transform="translate(${ox},${oy}) scale(${s})">
        <circle cx="12" cy="10" r="6" fill="#32CD32"/>
        <path d="M6 6 L4 2 L8 5 Z" fill="#228B22"/>
        <path d="M18 6 L20 2 L16 5 Z" fill="#228B22"/>
        <ellipse cx="9.5" cy="9" rx="2" ry="1.8" fill="white"/>
        <ellipse cx="14.5" cy="9" rx="2" ry="1.8" fill="white"/>
        <circle cx="10" cy="9" r="1" fill="#333"/>
        <circle cx="15" cy="9" r="1" fill="#333"/>
        <path d="M9 13 Q12 14.5 15 13" stroke="#333" stroke-width="0.5" fill="none"/>
        <ellipse cx="12" cy="18" rx="5" ry="6" fill="#32CD32"/>
        <path d="M5 12 Q0 8 3 5" stroke="#228B22" stroke-width="1.5" fill="none"/>
        <path d="M19 12 Q24 8 21 5" stroke="#228B22" stroke-width="1.5" fill="none"/>
        <path d="M7 10 L3 8" stroke="#228B22" stroke-width="0.8"/>
        <path d="M17 10 L21 8" stroke="#228B22" stroke-width="0.8"/>
        <rect x="7" y="24" width="3" height="3" rx="1" fill="#228B22"/>
        <rect x="14" y="24" width="3" height="3" rx="1" fill="#228B22"/>
      </g>`;

    default:
      return svg`<circle r="8" fill="#888" stroke="#111" stroke-width="1"/>`;
  }
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
    .object-dot { fill: #776; }
    .object-list-label { fill: #667; font-size: 5.5px; text-anchor: start; }
    .title { fill: #daa520; font-size: 14px; font-weight: 700; text-anchor: middle; font-family: Georgia, serif; letter-spacing: 2px; }
    .subtitle { fill: #666; font-size: 8px; text-anchor: middle; font-style: italic; }
    .floor-line { stroke: #444; stroke-width: 2; }
  `;

  render() {
    const w = 720, roomH = 80, rowGap = 14, topY = 3;
    const h = topY + 2 * roomH + rowGap + 20;
    const roomW = w / 3;

    const objectCounts: Record<string, number> = {};
    for (const room of this.rooms) { objectCounts[room.id] = room.objects.length; }
    const charPositions = layoutCharacters(this.characters, roomW, roomH, rowGap, topY, objectCounts);
    const roomIds = Object.keys(ROOM_GRID);

    return html`
      <svg viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg">
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
          <g class="char-group" style="transform: translate(${p.absX}px, ${p.baseY}px); cursor: pointer"
             @click=${(e: Event) => { e.stopPropagation(); this.dispatchEvent(new CustomEvent('character-selected', { detail: { characterId: p.id }, bubbles: true, composed: true })); }}>
            ${renderCharacterAtOrigin(p.id)}
            <text x="0" y="${14 + p.labelRow * 10}" class="char-name">${CHARACTER_LABELS[p.id] || p.name}</text>
          </g>
        `)}
      </svg>
    `;
  }

  private renderObjects(roomId: string, rx: number, roomY: number, _roomW: number) {
    const room = this.rooms.find(r => r.id === roomId);
    if (!room || room.objects.length === 0) return nothing;
    return room.objects.map((obj, i) => {
      const oy = roomY + 22 + i * 10;
      return svg`
        <circle cx="${rx + 6}" cy="${oy - 2}" r="2" class="object-dot" />
        <text x="${rx + 11}" y="${oy}" class="object-list-label">${obj.name.substring(0, 14)}</text>
      `;
    });
  }
}
