import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';

import { Alert } from 'app/shared/alert/alert';
import { AlertError } from 'app/shared/alert/alert-error';
import { ICourse } from '../course.model';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'jhi-course-detail',
  templateUrl: './course-detail.html',
  imports: [FontAwesomeModule, NgbTooltip, Alert, AlertError, RouterLink],
})
export class CourseDetail {
  readonly course = input<ICourse | null>(null);

  previousState(): void {
    globalThis.history.back();
  }
}
