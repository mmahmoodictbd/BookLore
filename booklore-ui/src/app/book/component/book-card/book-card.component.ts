import {Component, Input, OnInit} from '@angular/core';
import {Book} from '../../model/book.model';
import {Button} from 'primeng/button';
import {LibraryAndBookService} from '../../service/library-and-book.service';
import {Router} from '@angular/router';
import {MenuModule} from 'primeng/menu';
import {MenuItem} from 'primeng/api';
import {DialogService} from "primeng/dynamicdialog";
import {ShelfAssignerComponent} from "../shelf-assigner/shelf-assigner.component";

@Component({
    selector: 'app-book-card',
    templateUrl: './book-card.component.html',
    styleUrls: ['./book-card.component.scss'],
    imports: [
        Button,
        MenuModule
    ]
})
export class BookCardComponent implements OnInit {
    @Input() book!: Book;

    items: MenuItem[] | undefined;

    constructor(
        private libraryBookService: LibraryAndBookService, private router: Router,
        private dialogService: DialogService) {
    }

    coverImageSrc(book: Book): string {
        return this.libraryBookService.getBookCoverUrl(book.id);
    }

    readBook(book: Book) {
        this.libraryBookService.readBook(book);
    }

    openBookInfo(book: Book) {
        this.router.navigate(['/library', book.libraryId, 'book', book.id, 'info']);
    }

    ngOnInit(): void {
        this.items = [
            {
                label: 'Options',
                items: [
                    {
                        label: 'Add to shelf',
                        icon: 'pi pi-folder',
                        command: () => {
                            this.openShelfDialog(this.book);
                        }
                    },
                    {
                        label: 'View metadata',
                        icon: 'pi pi-info-circle',
                        command: () => {
                            this.openBookInfo(this.book);
                        }
                    }
                ]
            }
        ];
    }

    private openShelfDialog(book: Book) {
        this.dialogService.open(ShelfAssignerComponent, {
            header: 'Update Shelves: ' + book.metadata?.title,
            modal: true,
            width: '30%',
            height: '70%',
            contentStyle: {overflow: 'auto'},
            baseZIndex: 10,
            data: {
                book: this.book
            },
        });
    }
}
