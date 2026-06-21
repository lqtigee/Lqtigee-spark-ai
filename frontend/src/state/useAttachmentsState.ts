import { useCallback, useState } from "react";
import { deleteAttachment, uploadAttachment } from "../api/remoteApi";
import type { AttachmentDto } from "../types/api";

interface AttachmentsState {
  attachments: AttachmentDto[];
  attachmentIds: string[];
  uploading: boolean;
  deletingIds: Set<string>;
  error: unknown;
  uploadFile(file: File): Promise<void>;
  deleteUploadedAttachment(id: string): Promise<void>;
  clearAttachments(): void;
}

export function useAttachmentsState(): AttachmentsState {
  const [attachments, setAttachments] = useState<AttachmentDto[]>([]);
  const [uploading, setUploading] = useState(false);
  const [deletingIds, setDeletingIds] = useState<Set<string>>(() => new Set());
  const [error, setError] = useState<unknown>(null);

  const uploadFile = useCallback(async (file: File) => {
    setUploading(true);
    setError(null);

    try {
      const uploadedAttachment = await uploadAttachment(file);
      setAttachments((currentAttachments) => [...currentAttachments, uploadedAttachment]);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setUploading(false);
    }
  }, []);

  const deleteUploadedAttachment = useCallback(async (id: string) => {
    setDeletingIds((currentDeletingIds) => new Set(currentDeletingIds).add(id));
    setError(null);

    try {
      await deleteAttachment(id);
      setAttachments((currentAttachments) => currentAttachments.filter((attachment) => attachment.id !== id));
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setDeletingIds((currentDeletingIds) => {
        const nextDeletingIds = new Set(currentDeletingIds);
        nextDeletingIds.delete(id);
        return nextDeletingIds;
      });
    }
  }, []);

  const clearAttachments = useCallback(() => {
    setAttachments([]);
    setError(null);
  }, []);

  return {
    attachments,
    attachmentIds: attachments.map((attachment) => attachment.id),
    uploading,
    deletingIds,
    error,
    uploadFile,
    deleteUploadedAttachment,
    clearAttachments
  };
}
