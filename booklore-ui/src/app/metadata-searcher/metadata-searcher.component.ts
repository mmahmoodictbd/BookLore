import {Component} from '@angular/core';
import {Book, BookMetadata, FetchedMetadata} from '../book/model/book.model';
import {DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {BookService} from '../book/service/book.service';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-metadata-searcher',
  standalone: false,
  templateUrl: './metadata-searcher.component.html',
  styleUrls: ['./metadata-searcher.component.scss']
})
export class MetadataSearcherComponent {

  fetchedMetadata: FetchedMetadata;
  toUpdateMetadata: FetchedMetadata;
  book: Book;
  loading: boolean = false;
  updateThumbnail: boolean = false;

  constructor(public dynamicDialogConfig: DynamicDialogConfig, private dynamicDialogRef: DynamicDialogRef,
              private bookService: BookService, private messageService: MessageService) {

    this.fetchedMetadata = this.dynamicDialogConfig.data.fetchedMetadata;
    this.toUpdateMetadata = this.convertBookMetadataToFetchedMetadata(this.dynamicDialogConfig.data.currentMetadata);
    this.book = this.dynamicDialogConfig.data.book;
  }

  copyFetchedToCurrent(field: keyof FetchedMetadata) {
    if (this.fetchedMetadata && this.toUpdateMetadata) {
      this.toUpdateMetadata[field] = this.fetchedMetadata[field];
    }
  }

  get currentAuthorsString(): string {
    return this.toUpdateMetadata.authors ? this.toUpdateMetadata.authors.map(author => author).join(', ') : '';
  }

  set currentAuthorsString(value: string) {
    this.toUpdateMetadata.authors = value.split(',');
  }

  get fetchedAuthorsString(): string {
    return this.fetchedMetadata.authors ? this.fetchedMetadata.authors.map(author => author).join(', ') : '';
  }

  set fetchedAuthorsString(value: string) {
    this.fetchedMetadata.authors = value.split(',');
  }

  get currentCategoriesString(): string {
    return this.toUpdateMetadata.categories ? this.toUpdateMetadata.categories.map(category => category).join(', ') : '';
  }

  set currentCategoriesString(value: string) {
    this.toUpdateMetadata.categories = value.split(',');
  }

  get fetchedCategoriesString(): string {
    return this.fetchedMetadata.categories ? this.fetchedMetadata.categories.map(category => category).join(', ') : '';
  }

  set fetchedCategoriesString(value: string) {
    this.fetchedMetadata.categories = value.split(',');
  }

  closeDialog() {
    this.dynamicDialogRef.close();
  }

  coverImageSrc(book: Book): string {
    return this.bookService.getBookCoverUrl(book.id);
  }

  saveMetadata() {
    this.loading = true;
    this.updateFields();
    this.bookService.updateMetadata(this.book.id, this.toUpdateMetadata).subscribe({
      next: () => {
        this.messageService.add({severity: 'info', summary: 'Success', detail: 'Book metadata updated'});
        this.loading = false;
        this.dynamicDialogRef.close();
      },
      error: (error) => {
        this.loading = false;
        this.messageService.add({severity: 'error', summary: 'Error', detail: 'Failed to update book metadata'});
      }
    });
  }

  updateFields() {
    if (!this.toUpdateMetadata.title && this.fetchedMetadata.title) {
      this.toUpdateMetadata.title = this.fetchedMetadata.title;
    }

    if (!this.toUpdateMetadata.subtitle && this.fetchedMetadata.subtitle) {
      this.toUpdateMetadata.subtitle = this.fetchedMetadata.subtitle;
    }

    if (!this.toUpdateMetadata.publisher && this.fetchedMetadata.publisher) {
      this.toUpdateMetadata.publisher = this.fetchedMetadata.publisher;
    }

    if (!this.toUpdateMetadata.publishedDate && this.fetchedMetadata.publishedDate) {
      this.toUpdateMetadata.publishedDate = this.fetchedMetadata.publishedDate;
    }

    if (!this.toUpdateMetadata.description && this.fetchedMetadata.description) {
      this.toUpdateMetadata.description = this.fetchedMetadata.description;
    }

    if (!this.toUpdateMetadata.isbn10 && this.fetchedMetadata.isbn10) {
      this.toUpdateMetadata.isbn10 = this.fetchedMetadata.isbn10;
    }

    if (!this.toUpdateMetadata.language && this.fetchedMetadata.language) {
      this.toUpdateMetadata.language = this.fetchedMetadata.language;
    }

    if (!this.toUpdateMetadata.pageCount && this.fetchedMetadata.pageCount) {
      this.toUpdateMetadata.pageCount = this.fetchedMetadata.pageCount;
    }

    if (!this.toUpdateMetadata.authors || this.toUpdateMetadata.authors.length === 0) {
      this.toUpdateMetadata.authors = this.fetchedMetadata.authors;
    }

    if (!this.toUpdateMetadata.categories || this.toUpdateMetadata.categories.length === 0) {
      this.toUpdateMetadata.categories = this.fetchedMetadata.categories;
    }

    if (this.updateThumbnail) {
      this.toUpdateMetadata.thumbnailUrl = this.fetchedMetadata.thumbnailUrl;
    }
  }

  shouldUpdateThumbnail() {
    this.updateThumbnail = true;
  }

  convertBookMetadataToFetchedMetadata(bookMetadata: BookMetadata): FetchedMetadata {
    return {
      bookId: null,
      googleBookId: bookMetadata.googleBookId || null,
      amazonBookId: '',
      title: bookMetadata.title || null,
      subtitle: bookMetadata.subtitle || null,
      publisher: bookMetadata.publisher || null,
      publishedDate: bookMetadata.publishedDate || null,
      description: bookMetadata.description || null,
      isbn13: '',
      isbn10: bookMetadata.isbn10 || null,
      pageCount: bookMetadata.pageCount || null,
      thumbnailUrl: '',
      language: bookMetadata.language || null,
      rating: null,
      reviewCount: null,
      authors: bookMetadata.authors.map(author => author.name),
      categories: bookMetadata.categories.map(category => category.name),
    };
  }
}
