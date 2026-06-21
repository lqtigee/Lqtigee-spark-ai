import { useMemo, useState } from "react";

const DESTRUCTIVE_ACTIONS = new Set(["delete", "import"]);

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
  actionInFlight?: boolean;
  capabilitiesLoading?: boolean;
  capabilitiesError?: unknown;
  onStartAction?(action: string, confirmDestructive: boolean): Promise<void>;
}

export function SessionActionMenu({
  actions,
  actionInFlight = false,
  capabilitiesLoading = false,
  capabilitiesError = null,
  onStartAction
}: SessionActionMenuProps) {
  const [confirmation, setConfirmation] = useState<string | null>(null);
  const visibleActions = useMemo(
    () => Array.from(new Set(actions.map((action) => action.trim()).filter(Boolean))),
    [actions]
  );
  const regularActions = visibleActions.filter((action) => !DESTRUCTIVE_ACTIONS.has(action));
  const destructiveActions = visibleActions.filter((action) => DESTRUCTIVE_ACTIONS.has(action));
  const disabled = actionInFlight || capabilitiesLoading || Boolean(capabilitiesError) || !onStartAction;

  async function handleStartAction(action: string, confirmDestructive: boolean) {
    if (!onStartAction || disabled) {
      return;
    }
    await onStartAction(action, confirmDestructive);
    setConfirmation(null);
  }

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
          <ActionGroup
            actions={regularActions}
            actionInFlight={actionInFlight}
            confirmation={confirmation}
            disabled={disabled}
            onConfirm={setConfirmation}
            onStartAction={handleStartAction}
            title="常规操作"
          />
        ) : null}
        {destructiveActions.length > 0 ? (
          <ActionGroup
            actions={destructiveActions}
            actionInFlight={actionInFlight}
            confirmation={confirmation}
            destructive
            disabled={disabled}
            onConfirm={setConfirmation}
            onStartAction={handleStartAction}
            title="危险操作"
          />
        ) : null}
      </div>
    </details>
  );
}

function ActionGroup({
  actions,
  actionInFlight,
  confirmation,
  disabled,
  destructive = false,
  onConfirm,
  onStartAction,
  title
}: {
  actions: string[];
  actionInFlight: boolean;
  confirmation: string | null;
  disabled: boolean;
  destructive?: boolean;
  onConfirm(action: string | null): void;
  onStartAction(action: string, confirmDestructive: boolean): Promise<void>;
  title: string;
}) {
  return (
    <div className="session-action-menu__group">
      <span>{title}</span>
      {actions.map((action) => (
        <ActionButton
          action={action}
          actionInFlight={actionInFlight}
          confirmation={confirmation}
          disabled={disabled}
          destructive={destructive}
          key={action}
          onConfirm={onConfirm}
          onStartAction={onStartAction}
        />
      ))}
    </div>
  );
}

function ActionButton({
  action,
  actionInFlight,
  confirmation,
  disabled,
  destructive,
  onConfirm,
  onStartAction
}: {
  action: string;
  actionInFlight: boolean;
  confirmation: string | null;
  disabled: boolean;
  destructive: boolean;
  onConfirm(action: string | null): void;
  onStartAction(action: string, confirmDestructive: boolean): Promise<void>;
}) {
  const label = ACTION_LABELS[action] ?? action;

  if (!destructive) {
    return (
      <button className="session-action-menu__action" disabled={disabled} onClick={() => void onStartAction(action, false)} type="button">
        {actionInFlight ? "正在执行" : label}
      </button>
    );
  }

  if (destructive && confirmation !== action) {
    return (
      <button
        className="session-action-menu__action session-action-menu__action--destructive"
        disabled={disabled}
        onClick={() => onConfirm(action)}
        type="button"
      >
        {label}
      </button>
    );
  }

  return (
    <div className="session-action-menu__confirmation">
      <button
        className="session-action-menu__action session-action-menu__action--destructive"
        disabled={disabled}
        onClick={() => void onStartAction(action, true)}
        type="button"
      >
        {actionInFlight ? "正在执行" : `确认 ${label}`}
      </button>
      <button className="session-action-menu__cancel" disabled={actionInFlight} onClick={() => onConfirm(null)} type="button">
        取消
      </button>
    </div>
  );
}
