"use client";

import { useState } from "react";

/** Player avatar image with an initial-letter fallback. */
export default function Avatar({
  index,
  name,
  size = 36,
}: {
  index: number;
  name: string;
  size?: number;
}) {
  const [err, setErr] = useState(false);
  const n = (index % 6) + 1;
  if (err) {
    return (
      <div
        className="avatar-fallback"
        style={{ width: size, height: size, fontSize: size * 0.45 }}
      >
        {name.charAt(0).toUpperCase()}
      </div>
    );
  }
  return (
    <img
      src={`/avatars/a${n}.png`}
      alt={name}
      width={size}
      height={size}
      className="avatar-img"
      style={{ width: size, height: size }}
      onError={() => setErr(true)}
    />
  );
}
