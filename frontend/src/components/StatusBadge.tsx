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

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "活跃",
  CREATED: "已创建",
  EXITED: "已退出",
  FAILED: "失败",
  IDLE: "空闲",
  RUNNING: "运行中",
  STOPPED: "已停止",
  UNKNOWN: "未知",
  Connected: "已连接",
  unknown: "未知"
};

export function StatusBadge({ status, label }: StatusBadgeProps) {
  const normalizedStatus = status.trim().toUpperCase();
  const isKnown = normalizedStatus in STATUS_CLASSES;
  const displayLabel = STATUS_LABELS[label.trim()] ?? (isKnown ? STATUS_LABELS[normalizedStatus] : label.trim() || "未知");

  return (
    <span className={isKnown ? STATUS_CLASSES[normalizedStatus] : STATUS_CLASSES.UNKNOWN}>
      {displayLabel}
    </span>
  );
}
