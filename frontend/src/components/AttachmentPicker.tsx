import { useRef, type ChangeEvent } from "react";
import { ApiClientError } from "../api/httpClient";
import type { AttachmentDto } from "../types/api";

interface AttachmentPickerProps {
  attachments: AttachmentDto[];
  deletingIds: Set<string>;
  accept?: string;
  disabled?: boolean;
  uploading?: boolean;
  error?: unknown;
  onUpload(file: File): Promise<void>;
  onDelete(id: string): Promise<void>;
}

export function AttachmentPicker({
  attachments,
  deletingIds,
  accept,
  disabled = false,
  uploading = false,
  error = null,
  onUpload,
  onDelete
}: AttachmentPickerProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }
    await onUpload(file);
  }

  function openFilePicker() {
    if (disabled || uploading) {
      return;
    }
    fileInputRef.current?.click();
  }

  return (
    <section className="attachment-picker" aria-label="附件">
      <input
        ref={fileInputRef}
        accept={accept}
        className="attachment-picker__input"
        disabled={disabled || uploading}
        onChange={handleFileChange}
        type="file"
      />
      <button
        className="attachment-picker__button"
        disabled={disabled || uploading}
        onClick={openFilePicker}
        type="button"
      >
        {uploading ? "上传中" : "添加附件"}
      </button>
      {attachments.length > 0 ? (
        <ul className="attachment-picker__list" aria-label="已上传附件">
          {attachments.map((attachment) => (
            <li className="attachment-picker__item" key={attachment.id}>
              <div>
                <strong>{attachment.filename}</strong>
                <span>
                  {attachment.contentType} · {formatSize(attachment.sizeBytes)}
                </span>
              </div>
              <button
                className="attachment-picker__delete"
                disabled={disabled || deletingIds.has(attachment.id)}
                onClick={() => void onDelete(attachment.id)}
                type="button"
              >
                {deletingIds.has(attachment.id) ? "删除中" : "删除"}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {error ? <p className="attachment-picker__error">{formatAttachmentError(error)}</p> : null}
    </section>
  );
}

function formatAttachmentError(error: unknown): string {
  if (error instanceof ApiClientError) {
    return error.error.message || "附件操作失败";
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "附件操作失败";
}

function formatSize(sizeBytes: number): string {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`;
  }
  if (sizeBytes < 1024 * 1024) {
    return `${(sizeBytes / 1024).toFixed(1)} KB`;
  }
  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
}
