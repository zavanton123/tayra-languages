import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal';

import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { AlertError } from 'app/shared/alert/alert-error';
import { ILesson } from '../lesson.model';
import { LessonService } from '../service/lesson.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lesson-delete-dialog.html',
  imports: [FormsModule, FontAwesomeModule, AlertError],
})
export class LessonDeleteDialog {
  lesson?: ILesson;

  protected readonly lessonService = inject(LessonService);
  protected readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.lessonService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
