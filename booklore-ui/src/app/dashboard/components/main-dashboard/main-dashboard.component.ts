import {Component, inject, OnInit} from '@angular/core';
import {LibraryCreatorComponent} from '../../../book/components/library-creator/library-creator.component';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {LibraryService} from '../../../book/service/library.service';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {Button} from 'primeng/button';
import {AsyncPipe, NgIf} from '@angular/common';
import {DashboardScrollerComponent} from '../dashboard-scroller/dashboard-scroller.component';
import {BookService} from '../../../book/service/book.service';
import {BookState} from '../../../book/model/state/book-state.model';
import {Book} from '../../../book/model/book.model';
import {LibraryState} from '../../../book/model/state/library-state.model';

@Component({
  selector: 'app-main-dashboard',
  templateUrl: './main-dashboard.component.html',
  styleUrls: ['./main-dashboard.component.scss'],
  imports: [
    Button,
    NgIf,
    DashboardScrollerComponent,
    AsyncPipe
  ],
  providers: [DialogService],
})
export class MainDashboardComponent implements OnInit {
  ref: DynamicDialogRef | undefined;

  private bookService = inject(BookService);
  private libraryService = inject(LibraryService);
  private dialogService = inject(DialogService);

  lastReadBooks$: Observable<Book[]> | undefined;
  latestAddedBooks$: Observable<Book[]> | undefined;
  randomBooks$: Observable<Book[]> | undefined;
  libraryState$: Observable<LibraryState> | undefined = this.libraryService.libraryState$;

  ngOnInit(): void {
    this.bookService.loadBooks();

    this.lastReadBooks$ = this.bookService.bookState$.pipe(
      map((state: BookState) => (
        (state.books || []).filter(book => book.lastReadTime)
          .sort((a, b) => {
            const aTime = new Date(a.lastReadTime!).getTime();
            const bTime = new Date(b.lastReadTime!).getTime();
            return bTime - aTime;
          })
          .slice(0, 25)
      ))
    );

    this.latestAddedBooks$ = this.bookService.bookState$.pipe(
      map((state: BookState) => (
        (state.books || [])
          .filter(book => book.addedOn)
          .sort((a, b) => {
            const aTime = new Date(a.addedOn!).getTime();
            const bTime = new Date(b.addedOn!).getTime();
            return bTime - aTime;
          })
          .slice(0, 25)
      ))
    );

    this.randomBooks$ = this.bookService.bookState$.pipe(
      map((state: BookState) => {
        const books = state.books || [];
        return this.getRandomBooks(books, 10);
      })
    );
  }

  private getRandomBooks(books: Book[], count: number): Book[] {
    const shuffled = books.sort(() => 0.5 - Math.random());
    return shuffled.slice(0, count);
  }

  createNewLibrary() {
    this.ref = this.dialogService.open(LibraryCreatorComponent, {
      header: 'Create New Library',
      modal: true,
      closable: true,
      style: {
        position: 'absolute',
        top: '15%',
      }
    });
  }
}
