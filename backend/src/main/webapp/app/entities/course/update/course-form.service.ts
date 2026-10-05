import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ICourse, NewCourse } from '../course.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ICourse for edit and NewCourseFormGroupInput for create.
 */
type CourseFormGroupInput = ICourse | PartialWithRequiredKeyOf<NewCourse>;

type CourseFormDefaults = Pick<NewCourse, 'id' | 'published'>;

type CourseFormGroupContent = {
  id: FormControl<ICourse['id'] | NewCourse['id']>;
  slug: FormControl<ICourse['slug']>;
  languageCode: FormControl<ICourse['languageCode']>;
  title: FormControl<ICourse['title']>;
  description: FormControl<ICourse['description']>;
  level: FormControl<ICourse['level']>;
  topic: FormControl<ICourse['topic']>;
  sortOrder: FormControl<ICourse['sortOrder']>;
  published: FormControl<ICourse['published']>;
};

export type CourseFormGroup = FormGroup<CourseFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class CourseFormService {
  createCourseFormGroup(course?: CourseFormGroupInput): CourseFormGroup {
    const courseRawValue = {
      ...this.getFormDefaults(),
      ...(course ?? { id: null }),
    };
    return new FormGroup<CourseFormGroupContent>({
      id: new FormControl(
        { value: courseRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      slug: new FormControl(courseRawValue.slug, {
        validators: [
          Validators.required,
          Validators.maxLength(80),
          Validators.pattern('^[a-z0-9-]+$'), // NOSONAR
        ],
      }),
      languageCode: new FormControl(courseRawValue.languageCode, {
        validators: [Validators.required, Validators.minLength(2), Validators.maxLength(8)],
      }),
      title: new FormControl(courseRawValue.title, {
        validators: [Validators.required, Validators.maxLength(200)],
      }),
      description: new FormControl(courseRawValue.description, {
        validators: [Validators.maxLength(1000)],
      }),
      level: new FormControl(courseRawValue.level, {
        validators: [Validators.required],
      }),
      topic: new FormControl(courseRawValue.topic, {
        validators: [Validators.maxLength(100)],
      }),
      sortOrder: new FormControl(courseRawValue.sortOrder, {
        validators: [Validators.required, Validators.min(0)],
      }),
      published: new FormControl(courseRawValue.published, {
        validators: [Validators.required],
      }),
    });
  }

  getCourse(form: CourseFormGroup): ICourse | NewCourse {
    return form.getRawValue();
  }

  resetForm(form: CourseFormGroup, course: CourseFormGroupInput): void {
    const courseRawValue = { ...this.getFormDefaults(), ...course };
    form.reset({
      ...courseRawValue,
      id: { value: courseRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): CourseFormDefaults {
    return {
      id: null,
      published: false,
    };
  }
}
