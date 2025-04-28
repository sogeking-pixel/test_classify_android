package com.example.pruebaapp

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val images = arrayOf(

  R.drawable.bache_no_1,

  R.drawable.bache_si_1,

  R.drawable.animal_si_1,

  R.drawable.animal_no_1,

  R.drawable.animal_si_2,

  R.drawable.infraestructura_si_1,

  R.drawable.infraestructura_si_2,

  R.drawable.basura_si_1,

  R.drawable.desmonte_si_1,

  R.drawable.buzon_si_1,

  R.drawable.buzon_si_2,

)

val options = arrayOf(
  R.string.option_prompt_animal,
  R.string.option_prompt_basura,
  R.string.option_prompt_arbol,
  R.string.option_prompt_pista,
  R.string.option_prompt_alcantarillado,
  R.string.option_prompt_infraestructura
)

val prompts = arrayOf(
  R.string.prompt_maltrato,
  R.string.prompt_basura,
  R.string.prompt_arbol,
  R.string.prompt_pista,
  R.string.prompt_alcantarillado,
  R.string.prompt_infraestructura
)

@Composable
fun BakingScreen(
  modifier: Modifier = Modifier,
  bakingViewModel: BakingViewModel = viewModel()
) {
  var selectedImage by remember { mutableIntStateOf(0) }
  var selectedOption by rememberSaveable { mutableIntStateOf(0) }
  val prompt = stringResource(prompts[selectedOption])
  val placeholderResult = stringResource(R.string.results_placeholder)
  var result by rememberSaveable { mutableStateOf(placeholderResult) }
  val uiState by bakingViewModel.uiState.collectAsState()
  val imageState by bakingViewModel.imageState.collectAsState()
  var imageUri: Uri? by rememberSaveable { mutableStateOf(null) }
  val context = LocalContext.current
  val focusManager: FocusManager = LocalFocusManager.current
  val scope: CoroutineScope = rememberCoroutineScope()
  val photoLauncher: ManagedActivityResultLauncher<Uri, Boolean> =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture(), // Contract to take picture
      onResult = { success ->
        if (success) {
          imageUri?.let { uri ->
            bakingViewModel.takePhoto(uri)
          }
        }
        else {
          imageUri?.let { uri ->
            scope.launch {
              ComposeFileProvider.Companion.deleteFileFromCacheDirImages(context, uri)
            }
          }
        }
      },
    )


  val onTakePhotoLambda: () -> Unit = {
    focusManager.clearFocus()
    scope.launch {
        val fileUri: Uri = ComposeFileProvider.Companion.generateImageUri(context)
        imageUri = fileUri
        photoLauncher.launch(fileUri) // Launch the activity result contract
    }
  }



  Column(
    modifier = modifier
  ) {
    Text(
      text = stringResource(R.string.baking_title),
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(top = 40.dp, bottom = 20.dp)
    )

    ImagesList(
      modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
      images = images,
      selectedImage  = selectedImage,
      onSelectImage = { index -> selectedImage = index }
    )

    ElevatedButton(
      enabled = true,
      onClick = onTakePhotoLambda,
      modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = stringResource(R.string.take_photos)
      )
      Text(text = stringResource(R.string.take_photos))
    }

    OptionList(
      options = options,
      modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
      selectedOption = selectedOption,
      onOptionSelected = { index -> selectedOption = index }
    )

    Button(
      onClick = {
        val bitmap = BitmapFactory.decodeResource(
          context.resources,
          images[selectedImage]
        )
        bakingViewModel.sendPrompt(bitmap, prompt)
      },
      enabled = prompt.isNotEmpty(),
      modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
      Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = stringResource(R.string.action_label)
      )
      Text(text = stringResource(R.string.action_label))
    }


    if (uiState is UiState.Loading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    else {
      var textColor = MaterialTheme.colorScheme.onSurface
      if (uiState is UiState.Error) {
        textColor = MaterialTheme.colorScheme.error
        result = (uiState as UiState.Error).errorMessage
      } else if (uiState is UiState.Success) {
        textColor = MaterialTheme.colorScheme.onSurface
        result = (uiState as UiState.Success).outputText
      }
      val scrollState = rememberScrollState()
      Text(
        text = result,
        textAlign = TextAlign.Start,
        color = textColor,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(16.dp)
          .fillMaxSize()
          .verticalScroll(scrollState)
      )
    }

  }
}


@Composable
fun ImagesList(
  images: Array<Int>,
  modifier: Modifier = Modifier,
  selectedImage: Int = 0,
  onSelectImage : (Int) -> Unit = {},
){
  LazyRow(
    modifier = modifier
  ) {
    itemsIndexed(images) { index, image ->
      var imageModifier = Modifier
        .padding(start = 8.dp, end = 8.dp)
        .requiredSize(200.dp)
        .clickable {
          onSelectImage(index)
        }
      if (index == selectedImage) {
        imageModifier =
          imageModifier.border(BorderStroke(4.dp, MaterialTheme.colorScheme.primary))
      }
      Image(
        painter = painterResource(image),
        contentDescription = "",
        modifier = imageModifier
      )
    }
  }
}


@Composable
fun ImageElement(
  modifier: Modifier = Modifier,
  image: Int,
){
  Image(
    painter = painterResource(image),
    contentDescription = "",
    modifier = modifier
  )
}


@Composable
fun OptionList(
  options: Array<Int>,
  modifier: Modifier = Modifier,
  selectedOption: Int = 0,
  onOptionSelected: (Int) -> Unit = {}
){
  Column(
    modifier = modifier
  )
  {
    options.forEachIndexed { index, option ->
      Option(
        option = option,
        index = index,
        onOptionSelected = onOptionSelected,
        isOptionSelected = (index == selectedOption)
      )
    }
  }
}

@Composable
fun Option(
  option: Int,
  modifier: Modifier = Modifier,
  index: Int = 0,
  onOptionSelected: (Int) -> Unit = {},
  isOptionSelected: Boolean = false
){
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(selected = isOptionSelected, onClick = {onOptionSelected(index)})
    Text(
      text = stringResource(option)
    )
  }


}



@Preview(showSystemUi = true)
@Composable
fun BakingScreenPreview() {
    BakingScreen(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
    )
}