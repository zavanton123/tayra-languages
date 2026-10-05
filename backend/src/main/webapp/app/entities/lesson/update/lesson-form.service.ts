import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ILesson, NewLesson } from '../lesson.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ILesson for edit and NewLessonFormGroupInput for create.
 */
type LessonFormGroupInput = ILesson | PartialWithRequiredKeyOf<NewLesson>;

type LessonFormDefaults = Pick<NewLesson, 'id'>;

type LessonFormGroupContent = {
  id: FormControl<ILesson['id'] | NewLesson['id']>;
  slug: FormControl<ILesson['slug']>;
  title: FormControl<ILesson['title']>;
  summary: FormControl<ILesson['summary']>;
  content: FormControl<ILesson['content']>;
  sortOrder: FormControl<ILesson['sortOrder']>;
  course: FormControl<ILesson['course']>;
};

export type LessonFormGroup = FormGroup<LessonFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class LessonFormService {
  createLessonFormGroup(lesson?: LessonFormGroupInput): LessonFormGroup {
    const lessonRawValue = {
      ...this.getFormDefaults(),
      ...(lesson ?? { id: null }),
    };
    return new FormGroup<LessonFormGroupContent>({
      id: new FormControl(
        { value: lessonRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      slug: new FormControl(lessonRawValue.slug, {
        validators: [
          Validators.required,
          Validators.maxLength(80),
          Validators.pattern('^[a-z0-9-]+$'), // NOSONAR
        ],
      }),
      title: new FormControl(lessonRawValue.title, {
        validators: [Validators.required, Validators.maxLength(200)],
      }),
      summary: new FormControl(lessonRawValue.summary, {
        validators: [Validators.maxLength(500)],
      }),
      content: new FormControl(lessonRawValue.content, {
        validators: [Validators.required],
      }),
      sortOrder: new FormControl(lessonRawValue.sortOrder, {
        validators: [Validators.required, Validators.min(0)],
      }),
      course: new FormControl(lessonRawValue.course, {
        validators: [Validators.required],
      }),
    });
  }

  getLesson(form: LessonFormGroup): ILesson | NewLesson {
    return form.getRawValue();
  }

  resetForm(form: LessonFormGroup, lesson: LessonFormGroupInput): void {
    const lessonRawValue = { ...this.getFormDefaults(), ...lesson };
    form.reset({
      ...lessonRawValue,
      id: { value: lessonRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): LessonFormDefaults {
    return {
      id: null,
    };
  }
}
