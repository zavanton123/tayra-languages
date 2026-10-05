import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal';

import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { AlertError } from 'app/shared/alert/alert-error';
import { ICourse } from '../course.model';
import { CourseService } from '../service/course.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './course-delete-dialog.html',
  imports: [FormsModule, FontAwesomeModule, AlertError],
})
export class CourseDeleteDialog {
  course?: ICourse;

  protected readonly courseService = inject(CourseService);
  protected readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.courseService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
