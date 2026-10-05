import { HttpResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, FileLoadError } from 'app/core/util/data-util.service';
import { EventManager, EventWithContent } from 'app/core/util/event-manager.service';
import { ICourse } from 'app/entities/course/course.model';
import { CourseService } from 'app/entities/course/service/course.service';
import { AlertError } from 'app/shared/alert/alert-error';
import { AlertErrorModel } from 'app/shared/alert/alert-error.model';
import { ILesson } from '../lesson.model';
import { LessonService } from '../service/lesson.service';

import { LessonFormGroup, LessonFormService } from './lesson-form.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'jhi-lesson-update',
  templateUrl: './lesson-update.html',
  imports: [FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class LessonUpdate implements OnInit {
  readonly isSaving = signal(false);
  lesson: ILesson | null = null;

  coursesSharedCollection = signal<ICourse[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected lessonService = inject(LessonService);
  protected lessonFormService = inject(LessonFormService);
  protected courseService = inject(CourseService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: LessonFormGroup = this.lessonFormService.createLessonFormGroup();

  compareCourse = (o1: ICourse | null, o2: ICourse | null): boolean => this.courseService.compareCourse(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ lesson }) => {
      this.lesson = lesson;
      if (lesson) {
        this.updateForm(lesson);
      }

      this.loadRelationshipsOptions();
    });
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  setFileData(event: Event, field: string, isImage: boolean): void {
    this.dataUtils.loadFileToForm(event, this.editForm, field, isImage).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(new EventWithContent<AlertErrorModel>('tayraApp.error', { message: err.message })),
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const lesson = this.lessonFormService.getLesson(this.editForm);
    if (lesson.id === null) {
      this.subscribeToSaveResponse(this.lessonService.create(lesson));
    } else {
      this.subscribeToSaveResponse(this.lessonService.update(lesson));
    }
  }

  protected subscribeToSaveResponse(result: Observable<ILesson | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(lesson: ILesson): void {
    this.lesson = lesson;
    this.lessonFormService.resetForm(this.editForm, lesson);

    this.coursesSharedCollection.update(courses => this.courseService.addCourseToCollectionIfMissing<ICourse>(courses, lesson.course));
  }

  protected loadRelationshipsOptions(): void {
    this.courseService
      .query()
      .pipe(map((res: HttpResponse<ICourse[]>) => res.body ?? []))
      .pipe(map((courses: ICourse[]) => this.courseService.addCourseToCollectionIfMissing<ICourse>(courses, this.lesson?.course)))
      .subscribe((courses: ICourse[]) => this.coursesSharedCollection.set(courses));
  }
}
