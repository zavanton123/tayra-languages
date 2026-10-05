import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { isPresent } from 'app/core/util/operators';
import { ILesson, NewLesson } from '../lesson.model';

export type PartialUpdateLesson = Partial<ILesson> & Pick<ILesson, 'id'>;

@Injectable()
export class LessonsService {
  readonly lessonsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly lessonsResource = httpResource<ILesson[]>(() => {
    const params = this.lessonsParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of lesson that have been fetched. It is updated when the lessonsResource emits a new value.
   * In case of error while fetching the lessons, the signal is set to an empty array.
   */
  readonly lessons = computed(() => (this.lessonsResource.hasValue() ? this.lessonsResource.value() : []));
  protected readonly applicationConfigService = inject(ApplicationConfigService);
  protected readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/lessons');
}

@Injectable({ providedIn: 'root' })
export class LessonService extends LessonsService {
  protected readonly http = inject(HttpClient);

  create(lesson: NewLesson): Observable<ILesson> {
    return this.http.post<ILesson>(this.resourceUrl, lesson);
  }

  update(lesson: ILesson): Observable<ILesson> {
    return this.http.put<ILesson>(`${this.resourceUrl}/${encodeURIComponent(this.getLessonIdentifier(lesson))}`, lesson);
  }

  partialUpdate(lesson: PartialUpdateLesson): Observable<ILesson> {
    return this.http.patch<ILesson>(`${this.resourceUrl}/${encodeURIComponent(this.getLessonIdentifier(lesson))}`, lesson);
  }

  find(id: number): Observable<ILesson> {
    return this.http.get<ILesson>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<ILesson[]>> {
    const options = createRequestOption(req);
    return this.http.get<ILesson[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getLessonIdentifier(lesson: Pick<ILesson, 'id'>): number {
    return lesson.id;
  }

  compareLesson(o1: Pick<ILesson, 'id'> | null, o2: Pick<ILesson, 'id'> | null): boolean {
    return o1 && o2 ? this.getLessonIdentifier(o1) === this.getLessonIdentifier(o2) : o1 === o2;
  }

  addLessonToCollectionIfMissing<Type extends Pick<ILesson, 'id'>>(
    lessonCollection: Type[],
    ...lessonsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const lessons: Type[] = lessonsToCheck.filter(isPresent);
    if (lessons.length > 0) {
      const lessonCollectionIdentifiers = lessonCollection.map(lessonItem => this.getLessonIdentifier(lessonItem));
      const lessonsToAdd = lessons.filter(lessonItem => {
        const lessonIdentifier = this.getLessonIdentifier(lessonItem);
        if (lessonCollectionIdentifiers.includes(lessonIdentifier)) {
          return false;
        }
        lessonCollectionIdentifiers.push(lessonIdentifier);
        return true;
      });
      return [...lessonsToAdd, ...lessonCollection];
    }
    return lessonCollection;
  }
}
