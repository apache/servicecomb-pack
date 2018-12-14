/**
 * @license
 * Copyright Akveo. All Rights Reserved.
 * Licensed under the MIT License. See licenses/LICENSE-ngxadmin for license information.
 */
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'ngxPlural' })
export class PluralPipe implements PipeTransform {

  transform(input: number, label: string, pluralLabel: string = ''): string {
    input = input || 0;
    return input === 1
      ? `${input} ${label}`
      : pluralLabel
        ? `${input} ${pluralLabel}`
        : `${input} ${label}s`;
  }
}
