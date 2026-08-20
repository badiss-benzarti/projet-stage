/**
 * Declenche le telechargement d'un flux binaire renvoye par l'API.
 *
 * On passe par un blob plutot que par un lien direct : les endpoints de
 * telechargement exigent l'en-tete Authorization, qu'un <a href> ne peut
 * pas porter.
 */
export function telechargerBlob(blob: Blob, nomFichier: string): void {
  const url = URL.createObjectURL(blob);
  const lien = document.createElement('a');
  lien.href = url;
  lien.download = nomFichier;
  lien.click();
  URL.revokeObjectURL(url);
}
