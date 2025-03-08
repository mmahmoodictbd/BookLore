import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {NgxExtendedPdfViewerModule, PdfLoadedEvent, ScrollModeType} from 'ngx-extended-pdf-viewer';
import {BookService} from '../../service/book.service';
import {AppSettingsService} from '../../../core/service/app-settings.service';
import {filter, forkJoin, Subscription, take} from 'rxjs';
import {BookSetting, PdfViewerSetting} from '../../model/book.model';
import {PdfSettings} from '../../../core/model/app-settings.model';
import {UserService} from '../../../user.service';

@Component({
  selector: 'app-pdf-viewer',
  standalone: true,
  imports: [NgxExtendedPdfViewerModule],
  templateUrl: './pdf-viewer.component.html',
})
export class PdfViewerComponent implements OnInit, OnDestroy {
  handTool = true;
  rotation: 0 | 90 | 180 | 270 = 0;
  scrollMode: ScrollModeType = ScrollModeType.page;

  sidebarVisible!: boolean;
  page!: number;
  spread!: 'off' | 'even' | 'odd';
  zoom!: number | string;

  bookData!: string | Blob;
  bookId!: number;
  private appSettingsSubscription!: Subscription;

  private bookService = inject(BookService);
  private userService = inject(UserService);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.bookId = +params.get('bookId')!;

      const myself$ = this.userService.getMyself();
      const book$ = this.bookService.getBookByIdFromAPI(this.bookId, false);
      const bookSetting$ = this.bookService.getBookSetting(this.bookId);
      const pdfData$ = this.bookService.getFileContent(this.bookId);

      forkJoin([book$, bookSetting$, pdfData$, myself$]).subscribe((results) => {
        const pdfMeta = results[0];
        const pdfPrefs = results[1];
        const pdfData = results[2];
        const myself = results[3];

        let globalOrIndividual = myself.bookPreferences.perBookSetting.pdf;
        if (globalOrIndividual === 'Global') {
          this.zoom = myself.bookPreferences.pdfReaderSetting.pageZoom || 'page-fit';
          this.sidebarVisible = myself.bookPreferences.pdfReaderSetting.showSidebar ?? true;
          this.spread = myself.bookPreferences.pdfReaderSetting.pageSpread || 'odd';
        } else {
          this.zoom = pdfPrefs.pdfSettings?.zoom || myself.bookPreferences.pdfReaderSetting.pageZoom || 'page-fit';
          this.sidebarVisible = pdfPrefs.pdfSettings?.sidebarVisible ?? myself.bookPreferences.pdfReaderSetting.showSidebar ?? true;
          this.spread = pdfPrefs.pdfSettings?.spread || myself.bookPreferences.pdfReaderSetting.pageSpread || 'odd';
        }
        this.page = pdfMeta.pdfProgress || 1;
        this.bookData = pdfData;
      });
    });
  }

  onPageChange(page: number): void {
    if (page !== this.page) {
      this.page = page;
    }
    this.updateProgress();
  }

  onZoomChange(zoom: string | number): void {
    if (zoom !== this.zoom) {
      this.zoom = zoom;
      this.updateViewerSetting();
    }
  }

  onSidebarVisibleChange(visible: boolean): void {
    if (visible !== this.sidebarVisible) {
      this.sidebarVisible = visible;
      this.updateViewerSetting();
    }
  }

  onSpreadChange(spread: 'off' | 'even' | 'odd'): void {
    if (spread !== this.spread) {
      this.spread = spread;
      this.updateViewerSetting();
    }
  }

  private updateViewerSetting(): void {
    const bookSetting: BookSetting = {
      pdfSettings: {
        sidebarVisible: this.sidebarVisible,
        spread: this.spread,
        zoom: this.zoom,
      }
    }
    this.bookService.updateViewerSetting(bookSetting, this.bookId).subscribe();
  }

  updateProgress() {
    this.bookService.savePdfProgress(this.bookId, this.page).subscribe();
  }

  ngOnDestroy(): void {
    if (this.appSettingsSubscription) {
      this.appSettingsSubscription.unsubscribe();
    }
    this.updateProgress();
  }
}
