import { Routes } from '@angular/router';

import { ASC } from 'app/config/navigation.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import LessonResolve from './route/lesson-routing-resolve.service';

const lessonRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/lesson').then(m => m.Lesson),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/lesson-detail').then(m => m.LessonDetail),
    resolve: {
      lesson: LessonResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/lesson-update').then(m => m.LessonUpdate),
    resolve: {
      lesson: LessonResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/lesson-update').then(m => m.LessonUpdate),
    resolve: {
      lesson: LessonResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default lessonRoute;
