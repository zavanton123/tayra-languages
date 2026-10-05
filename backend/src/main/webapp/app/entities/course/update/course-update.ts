import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { Observable, finalize } from 'rxjs';

import { CourseLevel } from 'app/entities/enumerations/course-level.model';
import { AlertError } from 'app/shared/alert/alert-error';
import { ICourse } from '../course.model';
import { CourseService } from '../service/course.service';

import { CourseFormGroup, CourseFormService } from './course-form.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'jhi-course-update',
  templateUrl: './course-update.html',
  imports: [FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class CourseUpdate implements OnInit {
  readonly isSaving = signal(false);
  course: ICourse | null = null;
  courseLevelValues = Object.keys(CourseLevel);

  protected courseService = inject(CourseService);
  protected courseFormService = inject(CourseFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: CourseFormGroup = this.courseFormService.createCourseFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ course }) => {
      this.course = course;
      if (course) {
        this.updateForm(course);
      }
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const course = this.courseFormService.getCourse(this.editForm);
    if (course.id === null) {
      this.subscribeToSaveResponse(this.courseService.create(course));
    } else {
      this.subscribeToSaveResponse(this.courseService.update(course));
    }
  }

  protected subscribeToSaveResponse(result: Observable<ICourse | null>): void {
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

  protected updateForm(course: ICourse): void {
    this.course = course;
    this.courseFormService.resetForm(this.editForm, course);
  }
}
