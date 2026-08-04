export type ManorEvent =
  | { type: 'snapshot'; characters: CharacterSnapshot[]; rooms: RoomSnapshot[] }
  | { type: 'position'; characterId: string; room: string; x: number }
  | { type: 'dialogue'; characterId: string; room: string; content: string }
  | { type: 'aside'; characterId: string; content: string }
  | { type: 'narrator'; content: string }
  | { type: 'scene'; sceneId: string; status: 'started' | 'ended' }
  | { type: 'scenario'; status: 'started' | 'completed' }
  | { type: 'object'; objectId: string; room: string; visible: boolean; visibleTo?: string }
  | { type: 'control'; status: 'paused' | 'resumed' | 'speed'; speedMultiplier: number };

export interface CharacterSnapshot {
  id: string;
  name: string;
  room: string;
  x: number;
  active: boolean;
}

export interface RoomSnapshot {
  id: string;
  name: string;
  objects: ObjectSnapshot[];
}

export interface ObjectSnapshot {
  id: string;
  name: string;
  x: number;
}

export interface ChatMessage {
  characterId: string;
  room: string;
  content: string;
  timestamp: number;
  isAside?: boolean;
}

export const CHARACTER_COLORS: Record<string, string> = {
  'penelope-pitstop': '#ff69b4',
  'hooded-claw': '#2d5a27',
  'ant-hill-mob': '#8b4513',
  'dick-dastardly': '#6a0dad',
  'peter-perfect': '#4169e1',
  'narrator': '#daa520',
  'muttley': '#8B6914',
  'pat-pending': '#2E8B57',
  'sergeant-blast': '#556B2F',
  'private-meekly': '#6B8E23',
  'lazy-luke': '#DAA520',
  'blubber-bear': '#8B4513',
  'rock-slag': '#A0522D',
  'gravel-slag': '#708090',
  'rufus-ruffcut': '#B22222',
  'sawtooth': '#D2691E',
  'big-gruesome': '#9370DB',
  'little-gruesome': '#32CD32',
};

export const CHARACTER_SHORT_NAMES: Record<string, string> = {
  'penelope-pitstop': 'Penelope',
  'hooded-claw': 'Sneekly',
  'ant-hill-mob': 'Clyde',
  'dick-dastardly': 'Dastardly',
  'peter-perfect': 'Peter',
  'muttley': 'Muttley',
  'pat-pending': 'Pat',
  'sergeant-blast': 'Sgt. Blast',
  'private-meekly': 'Pvt. Meekly',
  'lazy-luke': 'Lazy Luke',
  'blubber-bear': 'Blubber',
  'rock-slag': 'Rock',
  'gravel-slag': 'Gravel',
  'rufus-ruffcut': 'Rufus',
  'sawtooth': 'Sawtooth',
  'big-gruesome': 'Big G',
  'little-gruesome': 'Lil G',
};

export interface CharacterProfileResponse {
  agentId: string;
  name: string;
  slot: string;
  slotLabel: string | null;
  enneagramType: string | null;
  dispositionProfile: Array<{ term: string; weight: number }>;
  capabilities: Array<{ name: string; tags: string[] }>;
  goals: Array<{ name: string; description: string; priority: string }>;
  constraints: Array<{ name: string; description: string; severity: string }>;
  briefing: string | null;
}
