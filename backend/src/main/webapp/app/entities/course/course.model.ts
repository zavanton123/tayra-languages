import { CourseLevel } from 'app/entities/enumerations/course-level.model';

export interface ICourse {
  id: number;
  slug?: string | null;
  languageCode?: string | null;
  title?: string | null;
  description?: string | null;
  level?: keyof typeof CourseLevel | null;
  topic?: string | null;
  sortOrder?: number | null;
  published?: boolean | null;
}

export type NewCourse = Omit<ICourse, 'id'> & { id: null };
