interface StatusBadgeProps {
  status: string;
  label: string;
}

const STATUS_CLASSES: Record<string, string> = {
  ACTIVE: "status-badge status-badge--active",
  CREATED: "status-badge status-badge--created",
  EXITED: "status-badge status-badge--exited",
  FAILED: "status-badge status-badge--failed",
  IDLE: "status-badge status-badge--idle",
  RUNNING: "status-badge status-badge--running",
  STOPPED: "status-badge status-badge--stopped",
  UNKNOWN: "status-badge status-badge--unknown"
};

export function StatusBadge({ status, label }: StatusBadgeProps) {
  const normalizedStatus = status.trim().toUpperCase();
  const isKnown = normalizedStatus in STATUS_CLASSES;

  return (
    <span className={isKnown ? STATUS_CLASSES[normalizedStatus] : STATUS_CLASSES.UNKNOWN}>
      {isKnown ? label : "unknown"}
    </span>
  );
}
