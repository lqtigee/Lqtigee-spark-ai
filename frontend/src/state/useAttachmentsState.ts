import { useCallback, useRef, useState } from "react";
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
  const scopeVersionRef = useRef(0);
  const deleteInFlightIdsRef = useRef<Set<string>>(new Set());

  const isCurrentScope = useCallback((version: number) => scopeVersionRef.current === version, []);

  const uploadFile = useCallback(async (file: File) => {
    const uploadScopeVersion = scopeVersionRef.current;
    setUploading(true);
    setError(null);

    try {
      const uploadedAttachment = await uploadAttachment(file);
      if (!isCurrentScope(uploadScopeVersion)) {
        return;
      }
      setAttachments((currentAttachments) => [...currentAttachments, uploadedAttachment]);
    } catch (caughtError) {
      if (!isCurrentScope(uploadScopeVersion)) {
        return;
      }
      setError(caughtError);
    } finally {
      if (isCurrentScope(uploadScopeVersion)) {
        setUploading(false);
      }
    }
  }, [isCurrentScope]);

  const deleteUploadedAttachment = useCallback(async (id: string) => {
    if (deleteInFlightIdsRef.current.has(id)) {
      return;
    }

    const deleteScopeVersion = scopeVersionRef.current;
    deleteInFlightIdsRef.current.add(id);
    setDeletingIds((currentDeletingIds) => new Set(currentDeletingIds).add(id));
    setError(null);

    try {
      await deleteAttachment(id);
      if (!isCurrentScope(deleteScopeVersion)) {
        return;
      }
      setAttachments((currentAttachments) => currentAttachments.filter((attachment) => attachment.id !== id));
    } catch (caughtError) {
      if (!isCurrentScope(deleteScopeVersion)) {
        return;
      }
      setError(caughtError);
    } finally {
      if (isCurrentScope(deleteScopeVersion)) {
        deleteInFlightIdsRef.current.delete(id);
        setDeletingIds((currentDeletingIds) => {
          const nextDeletingIds = new Set(currentDeletingIds);
          nextDeletingIds.delete(id);
          return nextDeletingIds;
        });
      }
    }
  }, [isCurrentScope]);

  const clearAttachments = useCallback(() => {
    scopeVersionRef.current += 1;
    deleteInFlightIdsRef.current.clear();
    setAttachments([]);
    setUploading(false);
    setDeletingIds(() => new Set());
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
