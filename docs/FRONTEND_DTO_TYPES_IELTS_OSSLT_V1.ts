// Frontend copy-ready DTO/enum definitions for OSSLT + Language Tracking v1.
// Source of truth: docs/osslt-language-tracking-openapi.yaml

export const IELTS_TRACKING_STATUS = [
  'GREEN_STRICT_PASS',
  'GREEN_COMMON_PASS_WITH_WARNING',
  'YELLOW_NEEDS_PREPARATION',
] as const;
export type IeltsTrackingStatus = (typeof IELTS_TRACKING_STATUS)[number];

export const LANGUAGE_SCORE_TRACKING_STATUS = [
  'TEACHER_REVIEW_APPROVED',
  'AUTO_PASS_ALL_SCHOOLS',
  'AUTO_PASS_PARTIAL_SCHOOLS',
  'NEEDS_TRACKING',
] as const;
export type LanguageScoreTrackingStatus = (typeof LANGUAGE_SCORE_TRACKING_STATUS)[number];

export const OSSLT_LATEST_RESULT = ['PASS', 'FAIL', 'UNKNOWN'] as const;
export type OssltLatestResult = (typeof OSSLT_LATEST_RESULT)[number];

export const OSSLT_TRACKING_STATUS = ['WAITING_UPDATE', 'NEEDS_TRACKING', 'PASSED'] as const;
export type OssltTrackingStatus = (typeof OSSLT_TRACKING_STATUS)[number];

export interface StudentIeltsModuleStateDTO {
  studentId: number;
  graduationYear: number | null;
  languageScoreType: 'IELTS' | 'TOEFL' | 'DUOLINGO';
  hasTakenIeltsAcademic: boolean | null;
  preparationIntent: string;

  // Canonical fields
  languageScoreTrackingManualStatus?: LanguageScoreTrackingStatus | null;
  languageScoreTrackingStatus?: LanguageScoreTrackingStatus | null;

  // Legacy aliases (transition only)
  languageTrackingManualStatus?: LanguageScoreTrackingStatus | null;
  languageTrackingStatus?: LanguageScoreTrackingStatus | null;

  trackingStatus?: IeltsTrackingStatus | null;
  updatedAt?: string | null;
}

export interface TeacherIeltsModuleUpdateRequestDTO {
  hasTakenIeltsAcademic?: boolean | null;
  languageScoreType?: 'IELTS' | 'TOEFL' | 'DUOLINGO';
  preparationIntent?: string;

  // Canonical (recommended)
  languageScoreTrackingManualStatus?: LanguageScoreTrackingStatus | null;

  // Legacy (compat only)
  languageTrackingManualStatus?: LanguageScoreTrackingStatus | null;
}

export interface StudentIeltsSummaryDTO {
  studentId: number;
  languageScoreType: 'IELTS' | 'TOEFL' | 'DUOLINGO';
  trackingStatus: IeltsTrackingStatus;
  languageScoreTrackingStatus?: LanguageScoreTrackingStatus | null;
  languageTrackingStatus?: LanguageScoreTrackingStatus | null;
}

export interface TeacherStudentOssltModuleStateDTO {
  studentId: number;
  graduationYear: number | null;
  latestOssltResult: OssltLatestResult;
  latestOssltDate: string | null; // yyyy-mm-dd
  hasOsslc: boolean | null;
  ossltTrackingManualStatus: OssltTrackingStatus | null;
  ossltTrackingStatus: OssltTrackingStatus;
  updatedAt: string | null;
}

export interface TeacherOssltModuleUpdateRequestDTO {
  latestOssltResult?: OssltLatestResult;
  latestOssltDate?: string | null; // yyyy-mm-dd | null | ""
  hasOsslc?: boolean | null;
  ossltTrackingManualStatus?: OssltTrackingStatus | null; // null | "" means clear
}

export interface StudentOssltModuleUpdateRequestDTO {
  latestOssltResult?: 'PASS' | 'FAIL';
  hasOsslc?: boolean;
}

export interface TeacherStudentOssltSummaryDTO {
  studentId: number;
  graduationYear: number | null;
  latestOssltResult: OssltLatestResult;
  latestOssltDate: string | null;
  hasOsslc: boolean | null;
  ossltTrackingManualStatus: OssltTrackingStatus | null;
  ossltTrackingStatus: OssltTrackingStatus;
  updatedAt: string | null;
}

export interface ApiErrorResponseDTO {
  status: number;
  message: string;
  code: 'BAD_REQUEST' | 'FORBIDDEN' | 'NOT_FOUND' | 'INTERNAL_ERROR' | string;
  details?: string[];
}

// Read strategy: new first, legacy fallback.
export function resolveLanguageScoreTrackingStatus(input: {
  languageScoreTrackingStatus?: LanguageScoreTrackingStatus | null;
  languageTrackingStatus?: LanguageScoreTrackingStatus | null;
}): LanguageScoreTrackingStatus | null {
  return input.languageScoreTrackingStatus ?? input.languageTrackingStatus ?? null;
}

// Read strategy: new first, legacy fallback.
export function resolveLanguageScoreTrackingManualStatus(input: {
  languageScoreTrackingManualStatus?: LanguageScoreTrackingStatus | null;
  languageTrackingManualStatus?: LanguageScoreTrackingStatus | null;
}): LanguageScoreTrackingStatus | null {
  return input.languageScoreTrackingManualStatus ?? input.languageTrackingManualStatus ?? null;
}
