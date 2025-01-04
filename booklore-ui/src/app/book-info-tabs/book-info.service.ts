import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {BookMetadataBI} from '../book/model/book-metadata-for-book-info.model';

@Injectable({
  providedIn: 'root'
})
export class BookInfoService {

  private bookMetadataSubject = new BehaviorSubject<BookMetadataBI | null>(null);
  bookMetadata$ = this.bookMetadataSubject.asObservable();

  private dialogCloseSubject = new BehaviorSubject<boolean | null>(null);
  dialogClose$ = this.dialogCloseSubject.asObservable();


  emit(bookMetadata: BookMetadataBI) {
    this.bookMetadataSubject.next(bookMetadata);
  }

  closeDialog(close: boolean) {
    return this.dialogCloseSubject.next(close);
  }
}
