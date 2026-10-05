import { ICourse, NewCourse } from './course.model';

export const sampleWithRequiredData: ICourse = {
  id: 8824,
  slug: 'is',
  languageCode: 'conserva',
  title: 'piglet helpful',
  level: 'B2',
  sortOrder: 27856,
  published: false,
};

export const sampleWithPartialData: ICourse = {
  id: 19981,
  slug: '2ce6',
  languageCode: 'brr when',
  title: 'zowie',
  description: 'miskey',
  level: 'B1',
  topic: 'excepting soupy actually',
  sortOrder: 4510,
  published: false,
};

export const sampleWithFullData: ICourse = {
  id: 30968,
  slug: 'k3',
  languageCode: 'that cha',
  title: 'drat develop after',
  description: 'upright down',
  level: 'A1',
  topic: 'whether underneath experienced',
  sortOrder: 15744,
  published: true,
};

export const sampleWithNewData: NewCourse = {
  slug: 'x',
  languageCode: 'pro alon',
  title: 'via tinted',
  level: 'A1',
  sortOrder: 18195,
  published: true,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
