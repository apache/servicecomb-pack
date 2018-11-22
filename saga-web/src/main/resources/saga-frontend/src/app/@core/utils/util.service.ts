import { Injectable } from '@angular/core';
import { ToasterService, ToasterConfig, Toast, BodyOutputType } from 'angular2-toaster';
import 'style-loader!angular2-toaster/toaster.css';

@Injectable({
  providedIn: 'root'
})
export class UtilService {

  constructor(private toasterService: ToasterService) { }

  config: ToasterConfig;

  positions = 'toast-bottom-right';
  animationType = 'fade';
  timeout = 5000;
  toastsLimit = 5;

  isNewestOnTop = true;
  isHideOnClick = true;
  isDuplicatesPrevented = false;
  isCloseButton = true;

/*   types: string[] = ['default', 'info', 'success', 'warning', 'error'];
  animations: string[] = ['fade', 'flyLeft', 'flyRight', 'slideDown', 'slideUp'];
  positions: string[] = ['toast-top-full-width', 'toast-bottom-full-width', 'toast-top-left', 'toast-top-center',
    'toast-top-right', 'toast-bottom-right', 'toast-bottom-center', 'toast-bottom-left', 'toast-center']; */

  success (title: string, body: string, params?: any) {
    this.config = new ToasterConfig({
      positionClass: params && params.position?params.position:this.positions,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      newestOnTop:  params && params.isNewestOnTop?params.isNewestOnTop:this.isNewestOnTop,
      tapToDismiss:  params && params.isHideOnClick?params.isHideOnClick:this.isHideOnClick,
      preventDuplicates:  params && params.isDuplicatesPrevented?params.isDuplicatesPrevented:this.isDuplicatesPrevented,
      animation:  params && params.animationType?params.animationType:this.animationType,
      limit:   params && params.toastsLimit?params.toastsLimit:this.toastsLimit,
    });
    const toast: Toast = {
      type: 'success',
      title: title,
      body: body,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      showCloseButton:  params && params.isCloseButton?params.isCloseButton:this.isCloseButton,
      bodyOutputType: BodyOutputType.TrustedHtml,
    };
    this.toasterService.popAsync(toast);
  }

  error (title: string, body: string, params?: any) {
    this.config = new ToasterConfig({
      positionClass: params && params.position?params.position:this.positions,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      newestOnTop:  params && params.isNewestOnTop?params.isNewestOnTop:this.isNewestOnTop,
      tapToDismiss:  params && params.isHideOnClick?params.isHideOnClick:this.isHideOnClick,
      preventDuplicates:  params && params.isDuplicatesPrevented?params.isDuplicatesPrevented:this.isDuplicatesPrevented,
      animation:  params && params.animationType?params.animationType:this.animationType,
      limit:   params && params.toastsLimit?params.toastsLimit:this.toastsLimit,
    });
    const toast: Toast = {
      type: 'error',
      title: title,
      body: body,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      showCloseButton:  params && params.isCloseButton?params.isCloseButton:this.isCloseButton,
      bodyOutputType: BodyOutputType.TrustedHtml,
    };
    this.toasterService.popAsync(toast);
  }

  warning (title: string, body: string, params?: any) {
    this.config = new ToasterConfig({
      positionClass: params && params.position?params.position:this.positions,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      newestOnTop:  params && params.isNewestOnTop?params.isNewestOnTop:this.isNewestOnTop,
      tapToDismiss:  params && params.isHideOnClick?params.isHideOnClick:this.isHideOnClick,
      preventDuplicates:  params && params.isDuplicatesPrevented?params.isDuplicatesPrevented:this.isDuplicatesPrevented,
      animation:  params && params.animationType?params.animationType:this.animationType,
      limit:   params && params.toastsLimit?params.toastsLimit:this.toastsLimit,
    });
    const toast: Toast = {
      type: 'warning',
      title: title,
      body: body,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      showCloseButton:  params && params.isCloseButton?params.isCloseButton:this.isCloseButton,
      bodyOutputType: BodyOutputType.TrustedHtml,
    };
    this.toasterService.popAsync(toast);
  }

  info (title: string, body: string, params?: any) {
    this.config = new ToasterConfig({
      positionClass: params && params.position?params.position:this.positions,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      newestOnTop:  params && params.isNewestOnTop?params.isNewestOnTop:this.isNewestOnTop,
      tapToDismiss:  params && params.isHideOnClick?params.isHideOnClick:this.isHideOnClick,
      preventDuplicates:  params && params.isDuplicatesPrevented?params.isDuplicatesPrevented:this.isDuplicatesPrevented,
      animation:  params && params.animationType?params.animationType:this.animationType,
      limit:   params && params.toastsLimit?params.toastsLimit:this.toastsLimit,
    });
    const toast: Toast = {
      type: 'info',
      title: title,
      body: body,
      timeout:  params && params.timeout?params.timeout:this.timeout,
      showCloseButton:  params && params.isCloseButton?params.isCloseButton:this.isCloseButton,
      bodyOutputType: BodyOutputType.TrustedHtml,
    };
    this.toasterService.popAsync(toast);
  }

  clearToasts() {
    this.toasterService.clear();
  }
}
