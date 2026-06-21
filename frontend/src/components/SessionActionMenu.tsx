import { useMemo, useState } from "react";

const DESTRUCTIVE_ACTIONS = new Set(["archive", "delete", "unarchive"]);

const ACTION_LABELS: Record<string, string> = {
  archive: "归档",
  delete: "删除",
  export: "导出",
  fork: "Fork",
  import: "导入",
  unarchive: "取消归档"
};

interface SessionActionMenuProps {
  actions: string[];
  capabilitiesLoading?: boolean;
  capabilitiesError?: unknown;
}

export function SessionActionMenu({ actions, capabilitiesLoading = false, capabilitiesError = null }: SessionActionMenuProps) {
  const [confirmation, setConfirmation] = useState<string | null>(null);
  const visibleActions = useMemo(() => actions.filter((action) => ACTION_LABELS[action]), [actions]);
  const regularActions = visibleActions.filter((action) => !DESTRUCTIVE_ACTIONS.has(action));
  const destructiveActions = visibleActions.filter((action) => DESTRUCTIVE_ACTIONS.has(action));

  return (
    <details className="session-action-menu">
      <summary>
        <span>操作</span>
      </summary>
      <div className="session-action-menu__panel">
        {capabilitiesLoading ? <p className="session-action-menu__empty">正在加载能力</p> : null}
        {capabilitiesError ? <p className="session-action-menu__error">能力加载失败</p> : null}
        {!capabilitiesLoading && !capabilitiesError && visibleActions.length === 0 ? (
          <p className="session-action-menu__empty">暂无可用操作</p>
        ) : null}
        {regularActions.length > 0 ? (
          <ActionGroup actions={regularActions} confirmation={confirmation} onConfirm={setConfirmation} title="常规操作" />
        ) : null}
        {destructiveActions.length > 0 ? (
          <ActionGroup actions={destructiveActions} confirmation={confirmation} destructive onConfirm={setConfirmation} title="危险操作" />
        ) : null}
      </div>
    </details>
  );
}

function ActionGroup({
  actions,
  confirmation,
  destructive = false,
  onConfirm,
  title
}: {
  actions: string[];
  confirmation: string | null;
  destructive?: boolean;
  onConfirm(action: string | null): void;
  title: string;
}) {
  return (
    <div className="session-action-menu__group">
      <span>{title}</span>
      {actions.map((action) => (
        <ActionButton
          action={action}
          confirmation={confirmation}
          destructive={destructive}
          key={action}
          onConfirm={onConfirm}
        />
      ))}
    </div>
  );
}

function ActionButton({
  action,
  confirmation,
  destructive,
  onConfirm
}: {
  action: string;
  confirmation: string | null;
  destructive: boolean;
  onConfirm(action: string | null): void;
}) {
  if (destructive && confirmation !== action) {
    return (
      <button className="session-action-menu__action session-action-menu__action--destructive" onClick={() => onConfirm(action)} type="button">
        {ACTION_LABELS[action]}
      </button>
    );
  }

  return (
    <button className={destructive ? "session-action-menu__action session-action-menu__action--destructive" : "session-action-menu__action"} disabled type="button">
      {destructive ? `确认 ${ACTION_LABELS[action]}` : ACTION_LABELS[action]}
    </button>
  );
}
