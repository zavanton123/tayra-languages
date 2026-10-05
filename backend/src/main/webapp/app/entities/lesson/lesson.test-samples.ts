import { ILesson, NewLesson } from './lesson.model';

export const sampleWithRequiredData: ILesson = {
  id: 5184,
  slug: 'r',
  title: 'and midst',
  content: '../fake-data/blob/hipster.txt',
  sortOrder: 2403,
};

export const sampleWithPartialData: ILesson = {
  id: 6599,
  slug: 'am',
  title: 'loosely stunning hungrily',
  content: '../fake-data/blob/hipster.txt',
  sortOrder: 28628,
};

export const sampleWithFullData: ILesson = {
  id: 29198,
  slug: 'erm',
  title: 'why toward whoever',
  summary: 'quizzically capitalise pluck',
  content: '../fake-data/blob/hipster.txt',
  sortOrder: 1091,
};

export const sampleWithNewData: NewLesson = {
  slug: 'qony',
  title: 'coolly carboxyl beyond',
  content: '../fake-data/blob/hipster.txt',
  sortOrder: 25029,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
