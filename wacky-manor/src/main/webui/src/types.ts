export type ManorEvent =
  | { type: 'snapshot'; characters: CharacterSnapshot[]; rooms: RoomSnapshot[] }
  | { type: 'position'; characterId: string; room: string; x: number }
  | { type: 'dialogue'; characterId: string; room: string; content: string }
  | { type: 'aside'; characterId: string; content: string }
  | { type: 'narrator'; content: string }
  | { type: 'scene'; sceneId: string; status: 'started' | 'ended' }
  | { type: 'scenario'; status: 'started' | 'completed' }
  | { type: 'object'; objectId: string; room: string; visible: boolean; visibleTo?: string };

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
};

export const CHARACTER_SHORT_NAMES: Record<string, string> = {
  'penelope-pitstop': 'Penelope',
  'hooded-claw': 'Sneekly',
  'ant-hill-mob': 'Clyde',
  'dick-dastardly': 'Dastardly',
  'peter-perfect': 'Peter',
};
