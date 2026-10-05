import { ICourse } from 'app/entities/course/course.model';

export interface ILesson {
  id: number;
  slug?: string | null;
  title?: string | null;
  summary?: string | null;
  content?: string | null;
  sortOrder?: number | null;
  course?: Pick<ICourse, 'id' | 'title'> | null;
}

export type NewLesson = Omit<ILesson, 'id'> & { id: null };
