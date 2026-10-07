import { API_URL } from "./config";

async function post(path: string) {
  const res = await fetch(`${API_URL}${path}`, { method: "POST" });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export const createRoom = (hostName: string): Promise<{ roomId: string; playerId: string }> =>
  post(`/api/rooms?gameCode=cotyphu&hostName=${encodeURIComponent(hostName)}`);

export const joinRoom = (roomId: string, name: string): Promise<{ playerId: string }> =>
  post(`/api/rooms/${roomId}/join?name=${encodeURIComponent(name)}`);

export const startGame = (roomId: string) =>
  post(`/api/rooms/${roomId}/start`);

export const leaveRoom = (roomId: string, playerId: string) =>
  fetch(`${API_URL}/api/rooms/${roomId}/leave?playerId=${playerId}`, {
    method: "DELETE",
  }).then((res) => {
    if (!res.ok) throw new Error("Rời phòng thất bại");
    return res.json();
  });

export async function getRoom(roomId: string) {
  const res = await fetch(`${API_URL}/api/rooms/${roomId}`);
  if (!res.ok) throw new Error("Không tìm thấy phòng");
  return res.json();
}
