import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { isPresent } from 'app/core/util/operators';
import { ICourse, NewCourse } from '../course.model';

export type PartialUpdateCourse = Partial<ICourse> & Pick<ICourse, 'id'>;

@Injectable()
export class CoursesService {
  readonly coursesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly coursesResource = httpResource<ICourse[]>(() => {
    const params = this.coursesParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of course that have been fetched. It is updated when the coursesResource emits a new value.
   * In case of error while fetching the courses, the signal is set to an empty array.
   */
  readonly courses = computed(() => (this.coursesResource.hasValue() ? this.coursesResource.value() : []));
  protected readonly applicationConfigService = inject(ApplicationConfigService);
  protected readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/courses');
}

@Injectable({ providedIn: 'root' })
export class CourseService extends CoursesService {
  protected readonly http = inject(HttpClient);

  create(course: NewCourse): Observable<ICourse> {
    return this.http.post<ICourse>(this.resourceUrl, course);
  }

  update(course: ICourse): Observable<ICourse> {
    return this.http.put<ICourse>(`${this.resourceUrl}/${encodeURIComponent(this.getCourseIdentifier(course))}`, course);
  }

  partialUpdate(course: PartialUpdateCourse): Observable<ICourse> {
    return this.http.patch<ICourse>(`${this.resourceUrl}/${encodeURIComponent(this.getCourseIdentifier(course))}`, course);
  }

  find(id: number): Observable<ICourse> {
    return this.http.get<ICourse>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<ICourse[]>> {
    const options = createRequestOption(req);
    return this.http.get<ICourse[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getCourseIdentifier(course: Pick<ICourse, 'id'>): number {
    return course.id;
  }

  compareCourse(o1: Pick<ICourse, 'id'> | null, o2: Pick<ICourse, 'id'> | null): boolean {
    return o1 && o2 ? this.getCourseIdentifier(o1) === this.getCourseIdentifier(o2) : o1 === o2;
  }

  addCourseToCollectionIfMissing<Type extends Pick<ICourse, 'id'>>(
    courseCollection: Type[],
    ...coursesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const courses: Type[] = coursesToCheck.filter(isPresent);
    if (courses.length > 0) {
      const courseCollectionIdentifiers = courseCollection.map(courseItem => this.getCourseIdentifier(courseItem));
      const coursesToAdd = courses.filter(courseItem => {
        const courseIdentifier = this.getCourseIdentifier(courseItem);
        if (courseCollectionIdentifiers.includes(courseIdentifier)) {
          return false;
        }
        courseCollectionIdentifiers.push(courseIdentifier);
        return true;
      });
      return [...coursesToAdd, ...courseCollection];
    }
    return courseCollection;
  }
}
