# Chave de assinatura do Hora Certa

O arquivo `horacerta-release.jks` contém a chave privada protegida por senha (formato PKCS#12). Guarde uma cópia em armazenamento privado. O arquivo local `keystore.properties` contém a senha de assinatura e deve ser guardado separadamente, em um gerenciador de senhas ou backup criptografado. Os dois arquivos ficam fora do repositório do aplicativo.

Para recuperar o ambiente de release, restaure os dois arquivos nesta pasta. O processo de build exige a chave existente e não cria uma substituta automaticamente. Toda atualização de uma instalação existente precisa usar a mesma chave de assinatura.

