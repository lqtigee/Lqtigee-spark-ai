import type { ApiErrorDto } from "../types/api";

interface ErrorPanelProps {
  title: string;
  error: unknown;
}

export function ErrorPanel({ title, error }: ErrorPanelProps) {
  const details = toErrorDetails(error);

  return (
    <section className="error-panel" aria-label={title}>
      <h3>{title}</h3>
      <dl className="definition-grid">
        <div>
          <dt>错误码</dt>
          <dd>{details.code}</dd>
        </div>
        <div>
          <dt>消息</dt>
          <dd>{details.message}</dd>
        </div>
        <div>
          <dt>详情</dt>
          <dd>{details.detail ?? "无"}</dd>
        </div>
      </dl>
    </section>
  );
}

function toErrorDetails(error: unknown): Pick<ApiErrorDto, "code" | "message" | "detail"> {
  if (isApiClientError(error)) {
    return {
      code: error.error.code,
      message: error.error.message,
      detail: error.error.detail
    };
  }
  if (error instanceof Error) {
    return {
      code: error.name,
      message: error.message,
      detail: null
    };
  }
  return {
    code: "UNKNOWN_ERROR",
    message: "未知错误",
    detail: null
  };
}

function isApiClientError(error: unknown): error is { error: ApiErrorDto } {
  return (
    typeof error === "object" &&
    error !== null &&
    "error" in error &&
    typeof (error as { error?: unknown }).error === "object" &&
    (error as { error?: unknown }).error !== null &&
    "code" in (error as { error: Record<string, unknown> }).error &&
    "message" in (error as { error: Record<string, unknown> }).error
  );
}
