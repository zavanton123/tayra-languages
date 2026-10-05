import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { Subject, from, of } from 'rxjs';

import { ICourse } from 'app/entities/course/course.model';
import { CourseService } from 'app/entities/course/service/course.service';
import { ILesson } from '../lesson.model';
import { LessonService } from '../service/lesson.service';

import { LessonFormService } from './lesson-form.service';
import { LessonUpdate } from './lesson-update';

describe('Lesson Management Update Component', () => {
  let comp: LessonUpdate;
  let fixture: ComponentFixture<LessonUpdate>;
  let activatedRoute: ActivatedRoute;
  let lessonFormService: LessonFormService;
  let lessonService: LessonService;
  let courseService: CourseService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(LessonUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    lessonFormService = TestBed.inject(LessonFormService);
    lessonService = TestBed.inject(LessonService);
    courseService = TestBed.inject(CourseService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Course query and add missing value', () => {
      const lesson: ILesson = { id: 25298 };
      const course: ICourse = { id: 2858 };
      lesson.course = course;

      const courseCollection: ICourse[] = [{ id: 2858 }];
      vitest.spyOn(courseService, 'query').mockReturnValue(of(new HttpResponse({ body: courseCollection })));
      const additionalCourses = [course];
      const expectedCollection: ICourse[] = [...additionalCourses, ...courseCollection];
      vitest.spyOn(courseService, 'addCourseToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ lesson });
      comp.ngOnInit();

      expect(courseService.query).toHaveBeenCalled();
      expect(courseService.addCourseToCollectionIfMissing).toHaveBeenCalledWith(
        courseCollection,
        ...additionalCourses.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.coursesSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const lesson: ILesson = { id: 25298 };
      const course: ICourse = { id: 2858 };
      lesson.course = course;

      activatedRoute.data = of({ lesson });
      comp.ngOnInit();

      expect(comp.coursesSharedCollection()).toContainEqual(course);
      expect(comp.lesson).toEqual(lesson);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<ILesson>();
      const lesson = { id: 5747 };
      vitest.spyOn(lessonFormService, 'getLesson').mockReturnValue(lesson);
      vitest.spyOn(lessonService, 'update').mockReturnValue(saveSubject);
      vitest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ lesson });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(lesson);
      saveSubject.complete();

      // THEN
      expect(lessonFormService.getLesson).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(lessonService.update).toHaveBeenCalledWith(expect.objectContaining(lesson));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<ILesson>();
      const lesson = { id: 5747 };
      vitest.spyOn(lessonFormService, 'getLesson').mockReturnValue({ id: null });
      vitest.spyOn(lessonService, 'create').mockReturnValue(saveSubject);
      vitest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ lesson: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(lesson);
      saveSubject.complete();

      // THEN
      expect(lessonFormService.getLesson).toHaveBeenCalled();
      expect(lessonService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<ILesson>();
      const lesson = { id: 5747 };
      vitest.spyOn(lessonService, 'update').mockReturnValue(saveSubject);
      vitest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ lesson });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(lessonService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareCourse', () => {
      it('should forward to courseService', () => {
        const entity = { id: 2858 };
        const entity2 = { id: 3722 };
        vitest.spyOn(courseService, 'compareCourse');
        comp.compareCourse(entity, entity2);
        expect(courseService.compareCourse).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
