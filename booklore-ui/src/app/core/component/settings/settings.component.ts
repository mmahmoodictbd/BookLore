import { Component } from '@angular/core';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {BookPreferences} from './book-preferences/book-preferences.component';
import {AdminComponent} from './admin/admin.component';

@Component({
  selector: 'app-settings',
  imports: [
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
    BookPreferences,
    AdminComponent
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {

}
