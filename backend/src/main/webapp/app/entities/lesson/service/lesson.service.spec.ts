import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ILesson } from '../lesson.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../lesson.test-samples';

import { LessonService } from './lesson.service';

const requireRestSample: ILesson = {
  ...sampleWithRequiredData,
};

describe('Lesson Service', () => {
  let service: LessonService;
  let httpMock: HttpTestingController;
  let expectedResult: ILesson | ILesson[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(LessonService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a Lesson', () => {
      const lesson = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(lesson).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Lesson', () => {
      const lesson = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(lesson).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Lesson', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Lesson', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Lesson', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests.length).toBe(1);
    });

    describe('addLessonToCollectionIfMissing', () => {
      it('should add a Lesson to an empty array', () => {
        const lesson: ILesson = sampleWithRequiredData;
        expectedResult = service.addLessonToCollectionIfMissing([], lesson);
        expect(expectedResult).toEqual([lesson]);
      });

      it('should not add a Lesson to an array that contains it', () => {
        const lesson: ILesson = sampleWithRequiredData;
        const lessonCollection: ILesson[] = [
          {
            ...lesson,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addLessonToCollectionIfMissing(lessonCollection, lesson);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Lesson to an array that doesn't contain it", () => {
        const lesson: ILesson = sampleWithRequiredData;
        const lessonCollection: ILesson[] = [sampleWithPartialData];
        expectedResult = service.addLessonToCollectionIfMissing(lessonCollection, lesson);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(lesson);
      });

      it('should add only unique Lesson to an array', () => {
        const lessonArray: ILesson[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const lessonCollection: ILesson[] = [sampleWithRequiredData];
        expectedResult = service.addLessonToCollectionIfMissing(lessonCollection, ...lessonArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const lesson: ILesson = sampleWithRequiredData;
        const lesson2: ILesson = sampleWithPartialData;
        expectedResult = service.addLessonToCollectionIfMissing([], lesson, lesson2);
        expect(expectedResult).toEqual([lesson, lesson2]);
      });

      it('should accept null and undefined values', () => {
        const lesson: ILesson = sampleWithRequiredData;
        expectedResult = service.addLessonToCollectionIfMissing([], null, lesson, undefined);
        expect(expectedResult).toEqual([lesson]);
      });

      it('should return initial array if no Lesson is added', () => {
        const lessonCollection: ILesson[] = [sampleWithRequiredData];
        expectedResult = service.addLessonToCollectionIfMissing(lessonCollection, undefined, null);
        expect(expectedResult).toEqual(lessonCollection);
      });
    });

    describe('compareLesson', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareLesson(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 5747 };
        const entity2 = null;

        const compareResult1 = service.compareLesson(entity1, entity2);
        const compareResult2 = service.compareLesson(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 5747 };
        const entity2 = { id: 25298 };

        const compareResult1 = service.compareLesson(entity1, entity2);
        const compareResult2 = service.compareLesson(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 5747 };
        const entity2 = { id: 5747 };

        const compareResult1 = service.compareLesson(entity1, entity2);
        const compareResult2 = service.compareLesson(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
