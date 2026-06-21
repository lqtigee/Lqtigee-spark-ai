import type { ChangeEvent } from "react";
import type { AttachmentDto } from "../types/api";

interface AttachmentPickerProps {
  attachments: AttachmentDto[];
  deletingIds: Set<string>;
  disabled?: boolean;
  uploading?: boolean;
  error?: unknown;
  onUpload(file: File): Promise<void>;
  onDelete(id: string): Promise<void>;
}

export function AttachmentPicker({
  attachments,
  deletingIds,
  disabled = false,
  uploading = false,
  error = null,
  onUpload,
  onDelete
}: AttachmentPickerProps) {
  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }
    await onUpload(file);
  }

  return (
    <section className="attachment-picker" aria-label="附件">
      <label className="attachment-picker__button">
        <input disabled={disabled || uploading} onChange={handleFileChange} type="file" />
        <span>{uploading ? "上传中" : "添加附件"}</span>
      </label>
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
      {error ? <p className="attachment-picker__error">附件操作失败</p> : null}
    </section>
  );
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
