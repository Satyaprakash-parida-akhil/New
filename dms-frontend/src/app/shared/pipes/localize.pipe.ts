import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
  name: 'localize',
  pure: false, // Set to false so it re-runs when the language changes
  standalone: true
})
export class LocalizePipe implements PipeTransform {
  constructor(private readonly translate: TranslateService) {}

  transform(value: any): any {
    if (value === null || value === undefined || value === '') return '';
    
    const str = String(value);
    
    // 1. Try to translate the string
    let translated = this.translate.instant(str);
    
    // 2. Guard: if the result is not a plain string (e.g. key maps to a JSON object),
    //    fall back to the original string so we never call .replace() on an object.
    if (typeof translated !== 'string') {
      translated = str;
    }
    
    // 3. Translate digits based on the current language
    const currentLang = this.translate.currentLang || 'en';
    
    if (currentLang === 'or') {
      translated = this.convertDigits(translated, ['୦', '୧', '୨', '୩', '୪', '୫', '୬', '୭', '୮', '୯']);
    } else if (currentLang === 'te') {
      translated = this.convertDigits(translated, ['౦', '౧', '౨', '౩', '౪', '౫', '౬', '౭', '౮', '౯']);
    } else if (currentLang === 'hi') {
      translated = this.convertDigits(translated, ['०', '१', '२', '३', '४', '५', '६', '७', '८', '९']);
    }
    
    return translated;
  }

  private convertDigits(str: string, nativeDigits: string[]): string {
    return str.replace(/[0-9]/g, (digit) => nativeDigits[parseInt(digit, 10)]);
  }
}
