import { useEffect, useState } from "react";
import { useOpencodeAgentsState } from "../state/useOpencodeAgentsState";
import type { SourceCapabilityDto } from "../types/api";

const OPENCODE_OPTIONS_KEY = "lqtigee_opencode_options";

interface OpencodeOptionsSheetProps {
  capability: SourceCapabilityDto;
}

interface StoredOpencodeOptions {
  agent?: string;
  fork?: boolean;
  replay?: boolean;
  replayLimit?: number;
  share?: boolean;
  thinking?: boolean;
  variant?: string;
}

export function OpencodeOptionsSheet({ capability }: OpencodeOptionsSheetProps) {
  const agentsState = useOpencodeAgentsState();
  const [options, setOptions] = useState<StoredOpencodeOptions>(() => readStoredOptions());
  const agentEnabled = capability.runOptions.includes("agent");
  const forkEnabled = capability.runOptions.includes("fork");
  const shareEnabled = capability.runOptions.includes("share");
  const variantEnabled = capability.runOptions.includes("variant");
  const thinkingEnabled = capability.runOptions.includes("thinking");
  const replayEnabled = capability.runOptions.includes("replay");
  const replayLimitEnabled = capability.runOptions.includes("replayLimit");
  const fileAttachmentsEnabled = capability.attachments.includes("file");

  useEffect(() => {
    if (agentEnabled) {
      void agentsState.loadAgents();
    }
  }, [agentEnabled, agentsState.loadAgents]);

  function updateOptions(nextPatch: StoredOpencodeOptions) {
    setOptions((currentOptions) => {
      const nextOptions = { ...currentOptions, ...nextPatch };
      localStorage.setItem(OPENCODE_OPTIONS_KEY, JSON.stringify(nextOptions));
      return nextOptions;
    });
  }

  return (
    <section className="options-sheet" aria-label="opencode 能力">
      <h4>opencode</h4>
      {agentEnabled ? (
        <label className="options-sheet__field">
          <span>代理</span>
          <select
            className="input-control"
            disabled={agentsState.loading || agentsState.agents.length === 0}
            onChange={(event) => updateOptions({ agent: event.target.value })}
            value={options.agent ?? ""}
          >
            <option value="">{agentsState.loading ? "正在加载代理" : "默认代理"}</option>
            {agentsState.agents.map((agent) => (
              <option key={agent.id} value={agent.id}>
                {agent.name} · {agent.source}
              </option>
            ))}
          </select>
        </label>
      ) : null}
      {agentsState.error ? <p className="options-sheet__error">代理加载失败</p> : null}
      {variantEnabled ? (
        <label className="options-sheet__field">
          <span>变体</span>
          <input className="input-control" onChange={(event) => updateOptions({ variant: event.target.value })} value={options.variant ?? ""} />
        </label>
      ) : null}
      <div className="options-sheet__toggles">
        {forkEnabled ? <Toggle checked={Boolean(options.fork)} label="派生" onChange={(enabled) => updateOptions({ fork: enabled })} /> : null}
        {shareEnabled ? <Toggle checked={Boolean(options.share)} label="共享" onChange={(enabled) => updateOptions({ share: enabled })} /> : null}
        {thinkingEnabled ? <Toggle checked={Boolean(options.thinking)} label="思考" onChange={(enabled) => updateOptions({ thinking: enabled })} /> : null}
        {replayEnabled ? <Toggle checked={options.replay ?? true} label="重放" onChange={(enabled) => updateOptions({ replay: enabled })} /> : null}
      </div>
      {replayLimitEnabled ? (
        <label className="options-sheet__field">
          <span>重放限制</span>
          <input
            className="input-control"
            max="200"
            min="1"
            onChange={(event) => updateOptions({ replayLimit: Number(event.target.value) })}
            type="number"
            value={options.replayLimit ?? 10}
          />
        </label>
      ) : null}
      {fileAttachmentsEnabled ? (
        <div className="options-sheet__group">
          <span>文件附件</span>
          <p className="options-sheet__hint">使用输入区下方的附件按钮上传文件，发送时会作为 opencode file 传入。</p>
        </div>
      ) : null}
    </section>
  );
}

function Toggle({ checked, label, onChange }: { checked: boolean; label: string; onChange(enabled: boolean): void }) {
  return (
    <label className="options-sheet__toggle">
      <input checked={checked} onChange={(event) => onChange(event.target.checked)} type="checkbox" />
      <span>{label}</span>
    </label>
  );
}

function readStoredOptions(): StoredOpencodeOptions {
  try {
    const rawValue = localStorage.getItem(OPENCODE_OPTIONS_KEY);
    if (!rawValue) {
      return { replay: true, replayLimit: 10 };
    }
    const parsed = JSON.parse(rawValue) as StoredOpencodeOptions;
    return parsed && typeof parsed === "object" ? { replay: true, replayLimit: 10, ...parsed } : { replay: true, replayLimit: 10 };
  } catch {
    return { replay: true, replayLimit: 10 };
  }
}
