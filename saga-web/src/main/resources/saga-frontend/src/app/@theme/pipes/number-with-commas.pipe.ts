/**
 * @license
 * Copyright Akveo. All Rights Reserved.
 * Licensed under the MIT License. See licenses/LICENSE-ngxadmin for license information.
 */
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'ngxNumberWithCommas' })
export class NumberWithCommasPipe implements PipeTransform {

  transform(input: number): string {
    return new Intl.NumberFormat().format(input);
  }
}
